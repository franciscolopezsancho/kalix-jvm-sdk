/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.akkasls.codegen
package java

import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.java.EntityServiceSourceGenerator.generateImports
import com.lightbend.akkasls.codegen.java.SourceGenerator._
import com.lightbend.akkasls.codegen.java.EntityServiceSourceGenerator.generateImports
import _root_.java.nio.file.{Files, Path}

object EventSourcedEntityTestKitGenerator {

  def generate(entity: ModelBuilder.Entity,
               service: ModelBuilder.EntityService,
               generatedSourceDirectory: Path): Iterable[Path] = {
    val packageName = entity.fqn.parent.javaPackage
    val className = entity.fqn.name
    val sourceCode = generateSource(service, entity, packageName, className)

    val packagePath = packageAsPath(packageName)
    val generatedSourceDirectoryPath = generatedSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))

    if (!generatedSourceDirectoryPath.toFile.exists) {
      Files.write(generatedSourceDirectoryPath, sourceCode.getBytes(Charsets.UTF_8))
      List(generatedSourceDirectoryPath)
    } else {
      Nil
    }

  }

  private[codegen] def generateSource(service: ModelBuilder.EntityService,
                                      entity: ModelBuilder.Entity,
                                      packageName: String,
                                      className: String): String = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity => generateSourceCode(service, entity, packageName, className)
      case entity: ModelBuilder.ValueEntity => "/** FIXME implement Value Entity testkit */"
    }
  }

  private[codegen] def generateSourceCode(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.EventSourcedEntity,
                                          packageName: String,
                                          className: String): String = {
    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl",
        "com.akkaserverless.javasdk.impl.effect.MessageReplyImpl",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl",
        "com.akkaserverless.javasdk.testkit.Result",
        "com.akkaserverless.javasdk.testkit.internal.AkkaServerlessTestKitHelper"
      )
    )

    val domainClassName = entity.fqn.parent.javaOuterClassname
    val entityClassName = entity.fqn.name
    val entityStateName = entity.state.fqn.name

    val testkitClassName = s"${entityClassName}TestKit"

    s"""$managedCodeCommentString
          |package ${entity.fqn.parent.pkg};
          |
          |import ${service.fqn.parent.pkg}.${service.fqn.parent.javaOuterClassname};
          |$imports
          |
          |public class ${testkitClassName} {
          |
          |    private ${domainClassName}.${entityStateName} state;
          |    private ${entityClassName} entity;
          |    private List<Object> events = new ArrayList<Object>();
          |    private AkkaServerlessTestKitHelper helper = new AkkaServerlessTestKitHelper<${domainClassName}.${entityStateName}>();
          |
          |    public ${testkitClassName}(${entityClassName} entity){
          |        this.state = entity.emptyState();
          |        this.entity = entity;
          |    }
          |
          |    public ${testkitClassName}(${entityClassName} entity, ${domainClassName}.${entityStateName} state){
          |        this.state = state;
          |        this.entity = entity;
          |    }
          |
          |    public ${domainClassName}.${entityStateName} getState(){
          |            return state;
          |    }
          |
          |    public List<Object> getAllEvents(){
          |        return this.events;
          |    }
          |
          |    private <Reply> List<Object> getEvents(EventSourcedEntity.Effect<Reply> effect){
          |        return CollectionConverters.asJava(helper.getEvents(effect));
          |    }
          |
          |    private <Reply> Reply getReplyOfType(EventSourcedEntity.Effect<Reply> effect, ${domainClassName}.${entityStateName} state){
          |        return (Reply) helper.getReply(effect, state);
          |    }
          |
          |    private ${domainClassName}.${entityStateName} handleEvent(${domainClassName}.${entityStateName} state, Object event) {
          |        ${Syntax.indent(generateHandleEvents(entity.events, domainClassName), 8)}
          |    }
          |
          |    private <Reply> Result<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect){
          |        List<Object> events = getEvents(effect); 
          |        this.events.add(events);
          |        for(Object e: events){
          |            this.state = handleEvent(state,e);
          |        }
          |        Reply reply = this.<Reply>getReplyOfType(effect, this.state);
          |        return new Result(reply, events);
          |    }
          |
          |    ${Syntax.indent(generateServices(service), 4)}
          |}""".stripMargin
  }

  def generateServices(service: ModelBuilder.EntityService): String = {
    require(!service.commands.isEmpty, "empty `commands` not allowed")

    val apiClassName = service.fqn.parent.javaOuterClassname

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        apiClassName + "." + command.outputType.name
      }

    service.commands
      .map { command =>
        s"""|public Result<${selectOutput(command)}> ${lowerFirst(command.fqn.name)}(${apiClassName}.${command.inputType.name} command) {
            |    EventSourcedEntity.Effect<${selectOutput(command)}> effect = entity.${lowerFirst(command.fqn.name)}(state, command);
            |    return interpretEffects(effect);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  //TODO This method should be deleted when the codegen CartHandler.handleEvents gets available
  def generateHandleEvents(events: Iterable[ModelBuilder.Event], domainClassName: String): String = {
    require(events.nonEmpty, "empty `events` not allowed")

    val top =
      s"""|if (event instanceof ${domainClassName}.${events.head.fqn.name}) {
          |    return entity.${lowerFirst(events.head.fqn.name)}(state, (${domainClassName}.${events.head.fqn.name}) event);
          |""".stripMargin

    val middle = events.tail.map { event =>
      s"""|} else if (event instanceof ${domainClassName}.${event.fqn.name}) {
          |    return entity.${lowerFirst(event.fqn.name)}(state, (${domainClassName}.${event.fqn.name}) event);""".stripMargin
    }

    val bottom =
      s"""
        |} else {
        |    throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        |}""".stripMargin

    top + middle.mkString("\n") + bottom
  }

}