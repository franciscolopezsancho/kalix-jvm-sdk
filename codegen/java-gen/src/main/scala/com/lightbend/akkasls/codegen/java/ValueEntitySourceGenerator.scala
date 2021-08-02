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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.Syntax
import com.lightbend.akkasls.codegen.Syntax._
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

object ValueEntitySourceGenerator {
  import SourceGenerator._

  private[codegen] def valueEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String
  ): String = {

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq.empty
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val methods = service.commands.map { cmd =>
      val methodName = cmd.fqn.name
      val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
      val outputType = qualifiedType(cmd.outputType)

      s"""|@Override
          |public Effect<$outputType> ${lowerFirst(methodName)}($outerClassAndState currentState, $inputType command) {
          |  return effects().error("The command handler for `$methodName` is not implemented, yet");
          |}
          |""".stripMargin
    }

    s"""|$generatedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity. */
        |public class $className extends Abstract${className} {
        |  @SuppressWarnings("unused")
        |  private final String entityId;
        |
        |  public ${className}(String entityId) {
        |    this.entityId = entityId;
        |  }
        |
        |  @Override
        |  public $outerClassAndState emptyState() {
        |    return ${outerClassAndState}.getDefaultInstance();
        |  }
        |
<<<<<<< HEAD
        |${Syntax.indent(methods, num = 2)}
=======
        |  ${Syntax.indent(methods, num = 2)}
>>>>>>> towards simplified codegen
        |}""".stripMargin
  }

  private[codegen] def valueEntityHandler(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.ValueEntity,
                                          packageName: String,
                                          className: String): String = {

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.AnySupport",
        "com.akkaserverless.javasdk.impl.EntityExceptions",
        "com.akkaserverless.javasdk.impl.valueentity.AdaptedCommandContextWithState",
        "com.akkaserverless.javasdk.lowlevel.ValueEntityHandler",
        "com.akkaserverless.javasdk.valueentity.CommandContext",
        "com.akkaserverless.javasdk.valueentity.ValueEntityBase",
        "com.google.protobuf.Any",
        "com.google.protobuf.Descriptors",
        "com.google.protobuf.GeneratedMessageV3",
        "java.util.Optional",
        "scalapb.UnknownFieldSet"
      )
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val cases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        s"""|case "$methodName":
            |  return entity.${lowerFirst(methodName)}(
            |      parsedState,
            |      ${inputType}.parseFrom(command.getValue()));
            |""".stripMargin
      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity handler */
        |public class ${className}Handler implements ValueEntityHandler {
        |
        |  public static final Descriptors.ServiceDescriptor serviceDescriptor =
        |      ${service.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${service.fqn.name}");
        |  public static final String entityType = "${entity.entityType}";
        |
        |  private final ${className} entity;
        |  
        |  public ${className}Handler(${className} entity) {
        |    this.entity = entity;
        |  }
        |
        |  @Override
        |  public ValueEntityBase.Effect<? extends GeneratedMessageV3> handleCommand(
        |      Any command, Any state, CommandContext<Any> context) throws Throwable {
        |      
        |    $outerClassAndState parsedState =
        |      $outerClassAndState.parseFrom(state.getValue());
        |
        |    CommandContext<$outerClassAndState> adaptedContext =
        |        new AdaptedCommandContextWithState(context, parsedState);
        |
        |    entity.setCommandContext(Optional.of(adaptedContext));
        |    
        |    try {
        |      switch (context.commandName()) {
        |
<<<<<<< HEAD
        |${Syntax.indent(cases, 8)}
=======
        |        ${Syntax.indent(cases, 8)}
>>>>>>> towards simplified codegen
        |
        |        default:
        |          throw new EntityExceptions.EntityException(
        |              context.entityId(),
        |              context.commandId(),
        |              context.commandName(),
        |              "No command handler found for command ["
        |                  + context.commandName()
        |                  + "] on "
        |                  + entity.getClass().toString());
        |      }
        |    } finally {
        |      entity.setCommandContext(Optional.empty());
        |    }
        |  }
        |  
        |  @Override
        |  public com.google.protobuf.any.Any emptyState() {
        |    return com.google.protobuf.any.Any.apply(
        |        AnySupport.DefaultTypeUrlPrefix()
        |          + "/"
        |          + ${outerClassAndState}.getDescriptor().getFullName(),
        |        entity.emptyState().toByteString(),
        |        UnknownFieldSet.empty());
        |  }
        |}""".stripMargin
<<<<<<< HEAD
=======

>>>>>>> towards simplified codegen
  }

  private[codegen] def abstractValueEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String
  ): String = {

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.valueentity.ValueEntityBase"
      )
    )

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name

        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        val outputType = qualifiedType(cmd.outputType)

        s"public abstract Effect<$outputType> ${lowerFirst(methodName)}($outerClassAndState currentState, $inputType ${lowerFirst(cmd.inputType.name)});"
      }

    s"""|$managedCodeCommentString
<<<<<<< HEAD
          |package $packageName;
          |
          |$imports
          |
          |/** A value entity. */
          |public abstract class Abstract${className} extends ValueEntityBase<$outerClassAndState> {
          |
          |${Syntax.indent(methods, 2)}
          |
          |}
          |""".stripMargin
=======
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity. */
        |public abstract class Abstract${className} extends ValueEntityBase<$outerClassAndState> {
        |
        |  ${Syntax.indent(methods, 2)}
        |
        |}""".stripMargin
>>>>>>> towards simplified codegen
  }
}
