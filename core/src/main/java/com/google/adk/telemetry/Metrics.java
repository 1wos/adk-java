/*
 * Copyright 2026 Google LLC
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

package com.google.adk.telemetry;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.adk.events.Event;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Utility class for recording OpenTelemetry metrics within the ADK. */
public final class Metrics {

  private static final AttributeKey<String> GEN_AI_AGENT_NAME =
      AttributeKey.stringKey("gen_ai.agent.name");
  private static final AttributeKey<String> GEN_AI_TOOL_NAME =
      AttributeKey.stringKey("gen_ai.tool.name");
  private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

  @SuppressWarnings("NonFinalStaticField")
  private static Meter meter = GlobalOpenTelemetry.getMeter("gcp.vertex.agent");

  @SuppressWarnings("NonFinalStaticField")
  private static DoubleHistogram agentInvocationDuration =
      meter
          .histogramBuilder("gen_ai.agent.invocation.duration")
          .setUnit("ms")
          .setDescription("Duration of agent invocations.")
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static DoubleHistogram toolExecutionDuration =
      meter
          .histogramBuilder("gen_ai.tool.execution.duration")
          .setUnit("ms")
          .setDescription("Duration of tool executions.")
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static LongHistogram agentRequestSize =
      meter
          .histogramBuilder("gen_ai.agent.request.size")
          .setUnit("By")
          .setDescription("Size of agent requests.")
          .ofLongs()
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static LongHistogram agentResponseSize =
      meter
          .histogramBuilder("gen_ai.agent.response.size")
          .setUnit("By")
          .setDescription("Size of agent responses.")
          .ofLongs()
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static LongHistogram agentWorkflowSteps =
      meter
          .histogramBuilder("gen_ai.agent.workflow.steps")
          .setUnit("1")
          .setDescription("Length of agentic workflow (# of events).")
          .ofLongs()
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static LongHistogram toolRequestSize =
      meter
          .histogramBuilder("gen_ai.tool.request.size")
          .setUnit("By")
          .setDescription("Size of tool requests.")
          .ofLongs()
          .build();

  @SuppressWarnings("NonFinalStaticField")
  private static LongHistogram toolResponseSize =
      meter
          .histogramBuilder("gen_ai.tool.response.size")
          .setUnit("By")
          .setDescription("Size of tool responses.")
          .ofLongs()
          .build();

  private Metrics() {}

  /** Sets the OpenTelemetry Meter to be used for metrics. This is for testing purposes only. */
  public static void setMeterForTesting(Meter meter) {
    Metrics.meter = meter;
    Metrics.agentInvocationDuration =
        meter
            .histogramBuilder("gen_ai.agent.invocation.duration")
            .setUnit("ms")
            .setDescription("Duration of agent invocations.")
            .build();
    Metrics.toolExecutionDuration =
        meter
            .histogramBuilder("gen_ai.tool.execution.duration")
            .setUnit("ms")
            .setDescription("Duration of tool executions.")
            .build();
    Metrics.agentRequestSize =
        meter
            .histogramBuilder("gen_ai.agent.request.size")
            .setUnit("By")
            .setDescription("Size of agent requests.")
            .ofLongs()
            .build();
    Metrics.agentResponseSize =
        meter
            .histogramBuilder("gen_ai.agent.response.size")
            .setUnit("By")
            .setDescription("Size of agent responses.")
            .ofLongs()
            .build();
    Metrics.agentWorkflowSteps =
        meter
            .histogramBuilder("gen_ai.agent.workflow.steps")
            .setUnit("1")
            .setDescription("Length of agentic workflow (# of events).")
            .ofLongs()
            .build();
    Metrics.toolRequestSize =
        meter
            .histogramBuilder("gen_ai.tool.request.size")
            .setUnit("By")
            .setDescription("Size of tool requests.")
            .ofLongs()
            .build();
    Metrics.toolResponseSize =
        meter
            .histogramBuilder("gen_ai.tool.response.size")
            .setUnit("By")
            .setDescription("Size of tool responses.")
            .ofLongs()
            .build();
  }

  /** Records the duration of the agent invocation. */
  public static void recordAgentInvocationDuration(
      String agentName, Duration duration, @Nullable Throwable error) {
    AttributesBuilder attrs = Attributes.builder().put(GEN_AI_AGENT_NAME, agentName);
    if (error != null) {
      attrs.put(ERROR_TYPE, error.getClass().getSimpleName());
    }
    agentInvocationDuration.record((double) duration.toMillis(), attrs.build());
  }

  /** Records the size of the agent request. */
  public static void recordAgentRequestSize(String agentName, @Nullable Content userContent) {
    long size = getContentSize(userContent);
    Attributes attrs = Attributes.of(GEN_AI_AGENT_NAME, agentName);
    agentRequestSize.record(size, attrs);
  }

  /** Records the size of the agent response by extracting content from events. */
  public static void recordAgentResponseSize(String agentName, @Nullable List<Event> events) {
    Content responseContent = null;
    if (events != null) {
      for (int i = events.size() - 1; i >= 0; i--) {
        Event event = events.get(i);
        if (agentName.equals(event.author()) && event.content().isPresent()) {
          responseContent = event.content().get();
          break;
        }
      }
    }
    long size = getContentSize(responseContent);
    Attributes attrs = Attributes.of(GEN_AI_AGENT_NAME, agentName);
    agentResponseSize.record(size, attrs);
  }

  /** Records the number of steps in the agent workflow by counting the number of events. */
  public static void recordAgentWorkflowSteps(String agentName, List<Event> events) {
    Attributes attrs = Attributes.of(GEN_AI_AGENT_NAME, agentName);
    long count =
        events.stream()
            .map(e -> e.stream().filter(event -> agentName.equals(event.author())).count())
            .orElse(0L);
    agentWorkflowSteps.record(count, attrs);
  }

  /** Records the duration of the tool execution. */
  public static void recordToolExecutionDuration(
      String toolName, String agentName, Duration duration, @Nullable Throwable error) {
    AttributesBuilder attrs =
        Attributes.builder().put(GEN_AI_AGENT_NAME, agentName).put(GEN_AI_TOOL_NAME, toolName);
    if (error != null) {
      attrs.put(ERROR_TYPE, error.getClass().getSimpleName());
    }
    toolExecutionDuration.record((double) duration.toMillis(), attrs.build());
  }

  /** Records the size of the tool request. */
  public static void recordToolRequestSize(
      String toolName, String agentName, Map<String, Object> functionArgs) {
    long size = 0;
    for (Object value : functionArgs.values()) {
      if (value instanceof String s) {
        size += s.getBytes(UTF_8).length;
      }
    }
    Attributes attrs = Attributes.of(GEN_AI_TOOL_NAME, toolName, GEN_AI_AGENT_NAME, agentName);
    toolRequestSize.record(size, attrs);
  }

  /** Records the size of the tool response. */
  public static void recordToolResponseSize(
      String toolName, String agentName, @Nullable Event responseEvent) {
    long size = 0;
    if (responseEvent != null) {
      size = getContentSize(responseEvent.content().orElse(null));
    }
    Attributes attrs = Attributes.of(GEN_AI_TOOL_NAME, toolName, GEN_AI_AGENT_NAME, agentName);
    toolResponseSize.record(size, attrs);
  }

  private static long getContentSize(@Nullable Content content) {
    if (content == null) {
      return 0;
    }
    long size = 0;
    for (Part part : content.parts().orElse(ImmutableList.of())) {
      size += part.text().map(s -> s.getBytes(UTF_8).length).orElse(0);
      size +=
          part.inlineData()
              .flatMap(inlineData -> inlineData.data())
              .map(data -> data.length)
              .orElse(0);
    }
    return size;
  }
}
