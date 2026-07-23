package com.microsoft.financecopilot.ai.config;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;

/**
 * Adds a {@code use_case} low-cardinality tag to Spring AI's built-in {@code spring.ai.chat.client}
 * observation, so {@code /actuator/metrics} can distinguish nl2sql/anomaly/report calls. One
 * instance per {@code ChatClient} bean, since each is dedicated to a single use case (see {@link
 * ChatClientConfig}). {@code model} is already tagged by Spring AI's default {@code ChatModel}
 * observation convention (as {@code gen_ai.request.model}) — no code needed there.
 */
public class UseCaseTaggingObservationConvention extends DefaultChatClientObservationConvention {

  private final String useCase;

  public UseCaseTaggingObservationConvention(String useCase) {
    this.useCase = useCase;
  }

  @Override
  public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext context) {
    return super.getLowCardinalityKeyValues(context).and(KeyValue.of("use_case", useCase));
  }
}
