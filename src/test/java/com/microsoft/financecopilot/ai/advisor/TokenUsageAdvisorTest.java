package com.microsoft.financecopilot.ai.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

class TokenUsageAdvisorTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final TokenUsageAdvisor advisor = new TokenUsageAdvisor(meterRegistry, "nl2sql");

  @Test
  void publishesPromptAndCompletionTokenCountersTaggedByUseCase() {
    ChatResponse chatResponse =
        new ChatResponse(
            java.util.List.of(new Generation(new AssistantMessage("hi"))),
            ChatResponseMetadata.builder().usage(new DefaultUsage(120, 45)).build());
    ChatClientResponse response =
        ChatClientResponse.builder().chatResponse(chatResponse).context(java.util.Map.of()).build();

    advisor.after(response, null);

    assertThat(meterRegistry.counter("ai.tokens.prompt", "use_case", "nl2sql").count())
        .isEqualTo(120.0);
    assertThat(meterRegistry.counter("ai.tokens.completion", "use_case", "nl2sql").count())
        .isEqualTo(45.0);
  }

  @Test
  void doesNotFailWhenUsageMetadataIsAbsent() {
    ChatResponse chatResponse =
        new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("hi"))));
    ChatClientResponse response =
        ChatClientResponse.builder().chatResponse(chatResponse).context(java.util.Map.of()).build();

    ChatClientResponse result = advisor.after(response, null);

    assertThat(result).isSameAs(response);
  }
}
