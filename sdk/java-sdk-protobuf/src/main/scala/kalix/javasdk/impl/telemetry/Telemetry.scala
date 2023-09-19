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

package kalix.javasdk.impl.telemetry

import akka.actor.ActorSystem.Settings
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.{ Context => OtelContext }
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import kalix.javasdk.Metadata
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.Service
import kalix.protocol.action.ActionCommand
import kalix.protocol.entity.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.OptionConverters._

object Telemetry {

  val TRACE_PARENT_KEY = "traceparent"

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val otelGetter = new TextMapGetter[Metadata]() {
    override def get(carrier: Metadata, key: String): String = {
      logger.debug("For the key [{}] the value is [{}]", key, carrier.get(key))
      carrier.get(key).toScala.getOrElse("")
    }

    override def keys(carrier: Metadata): java.lang.Iterable[String] =
      carrier.getAllKeys
  }

  val sdkTracesSet = new ConcurrentHashMap[OpenTelemetrySdk, Boolean]().keySet(true)

  Runtime
    .getRuntime()
    .addShutdownHook(new Thread(new Runnable {
      def run(): Unit = {
        sdkTracesSet.forEach(_.close())
      }
    }))
}

class Telemetry(serviceName: String, settings: Settings) {

  import Telemetry._

  private val openTelemetry: OpenTelemetry = {

    val resource =
      Resource.getDefault.merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)))
    val sdkTracerProvider =
      SdkTracerProvider
        .builder()
        .addSpanProcessor(
          SimpleSpanProcessor.create(
            OtlpGrpcSpanExporter
              .builder()
              .setEndpoint(settings.config.getString("kalix.telemetry.tracing.collector-endpoint"))
              .build()))
        .setResource(resource)
        .build()
    val sdk = OpenTelemetrySdk
      .builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build()

    sdkTracesSet.add(sdk)
    sdk
  }

  /**
   * Creates a span if it finds a trace parent in the command's metadata
   * @param service
   * @param command
   * @return
   */
  def buildSpan(service: Service, command: Command): Option[Span] = {
    val metadata = new MetadataImpl(command.metadata.map(_.entries).getOrElse(Nil))
    if (metadata.get(TRACE_PARENT_KEY).isPresent) {
      logger.debug(" commandName content [{}].", command)

      val context = openTelemetry.getPropagators.getTextMapPropagator
        .extract(OtelContext.current(), metadata, otelGetter.asInstanceOf[TextMapGetter[Object]])
      val tracer = openTelemetry.getTracer("java-sdk")
      val span = tracer
        .spanBuilder(s"""${service.serviceName}.${command.entityId}""")
        .setParent(context)
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
      Some(
        span
          .setAttribute("service.name", s"""${service.serviceName}.${command.entityId}""")
          .setAttribute(s"kalix.${service.componentType}", command.entityId))
    } else {
      logger.debug("No trace parent found.")
      None
    }
  }

  def buildSpan(service: Service, command: ActionCommand): Option[Span] = {
    val metadata = new MetadataImpl(command.metadata.map(_.entries).getOrElse(Nil))
    if (metadata.get(TRACE_PARENT_KEY).isPresent) {
      logger.debug(" actionCommand content [{}].", command)

      val context = openTelemetry.getPropagators.getTextMapPropagator
        .extract(OtelContext.current(), metadata, otelGetter.asInstanceOf[TextMapGetter[Object]])
      val tracer = openTelemetry.getTracer("java-sdk")
      val span = tracer
        .spanBuilder(s"""${command.serviceName}.${command.name}""")
        .setParent(context)
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
      Some(
        span
          .setAttribute("service.name", s"""${service.serviceName}""")
          .setAttribute(s"kalix.${service.componentType}", command.name))
    } else {
      logger.debug("No trace parent found.")
      None
    }

  }

}