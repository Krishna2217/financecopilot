package com.microsoft.financecopilot.ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Verifies the three per-use-case {@code ChatClient} beans build cleanly against a stubbed {@code
 * ChatModel} (no real Azure OpenAI credentials required), per the Phase 2 DoD.
 */
@SpringJUnitConfig(
    classes = {ChatClientConfig.class, ChatClientConfigTest.StubbedModelConfig.class})
class ChatClientConfigTest {

  @Autowired
  @Qualifier("nl2sqlChatClient")
  private ChatClient nl2sqlChatClient;

  @Autowired
  @Qualifier("anomalyChatClient")
  private ChatClient anomalyChatClient;

  @Autowired
  @Qualifier("reportChatClient")
  private ChatClient reportChatClient;

  @Test
  void allThreeUseCaseChatClientsLoadCleanly() {
    assertThat(nl2sqlChatClient).isNotNull();
    assertThat(anomalyChatClient).isNotNull();
    assertThat(reportChatClient).isNotNull();
    assertThat(nl2sqlChatClient).isNotSameAs(anomalyChatClient).isNotSameAs(reportChatClient);
  }

  @Configuration
  static class StubbedModelConfig {

    @Bean
    ChatModel chatModel() {
      return mock(ChatModel.class);
    }

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    VectorStore vectorStore() {
      return mock(VectorStore.class);
    }

    @Bean
    ObservationRegistry observationRegistry() {
      return ObservationRegistry.create();
    }

    @Bean
    PromptProperties promptProperties() {
      return new PromptProperties(
          new ClassPathResource("prompts/nl2sql.st"),
          new ClassPathResource("prompts/anomaly-explain.st"),
          new ClassPathResource("prompts/exec-report.st"));
    }
  }
}
