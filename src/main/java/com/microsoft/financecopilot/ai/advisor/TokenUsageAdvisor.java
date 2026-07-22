package com.microsoft.financecopilot.ai.advisor;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;

/**
 * Publishes {@code ai.tokens.prompt}/{@code ai.tokens.completion} Micrometer counters (tagged by
 * use case) after each model call, and mirrors the same values into MDC so the request's log lines
 * carry token spend. One instance is created per {@code ChatClient} use case (see {@code
 * com.microsoft.financecopilot.ai.config.ChatClientConfig}) so the {@code use_case} tag is fixed at
 * construction time.
 */
public class TokenUsageAdvisor implements BaseAdvisor {

  private final MeterRegistry meterRegistry;
  private final String useCase;

  public TokenUsageAdvisor(MeterRegistry meterRegistry, String useCase) {
    this.meterRegistry = meterRegistry;
    this.useCase = useCase;
  }

  @Override
  public String getName() {
    return "TokenUsageAdvisor[" + useCase + "]";
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    return request;
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    if (response.chatResponse() == null || response.chatResponse().getMetadata() == null) {
      return response;
    }
    Usage usage = response.chatResponse().getMetadata().getUsage();
    if (usage == null) {
      return response;
    }
    int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
    int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

    MDC.put("promptTokens", String.valueOf(promptTokens));
    MDC.put("completionTokens", String.valueOf(completionTokens));
    meterRegistry.counter("ai.tokens.prompt", "use_case", useCase).increment(promptTokens);
    meterRegistry.counter("ai.tokens.completion", "use_case", useCase).increment(completionTokens);

    return response;
  }
}
