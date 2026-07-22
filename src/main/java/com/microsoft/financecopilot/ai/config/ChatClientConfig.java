package com.microsoft.financecopilot.ai.config;

import com.microsoft.financecopilot.ai.advisor.PromptGuardAdvisor;
import com.microsoft.financecopilot.ai.advisor.TokenUsageAdvisor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Produces one {@code ChatClient} bean per AI use case, each with its own system prompt (loaded
 * from {@code src/main/resources/prompts/*.st} via {@link PromptTemplate}) and its own advisor
 * chain. {@code QuestionAnswerAdvisor} (schema RAG) is added to {@code nl2sqlChatClient} in Phase
 * 2.5 once the vector store is populated.
 *
 * <p>Gated by {@code app.ai.enabled} (default {@code true}) rather than
 * {@code @ConditionalOnBean(ChatModel.class)}: Spring Boot processes user {@code @Configuration}
 * classes before autoconfiguration registers the model's {@code ChatModel} bean, so a bean-presence
 * condition here could evaluate before that bean exists even when real credentials are configured.
 * The {@code local} profile sets {@code app.ai.enabled: false} since it has no Azure/OpenAI
 * credentials, so the app still boots for non-AI work.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ChatClientConfig {

  private final PromptProperties promptProperties;
  private final MeterRegistry meterRegistry;

  public ChatClientConfig(PromptProperties promptProperties, MeterRegistry meterRegistry) {
    this.promptProperties = promptProperties;
    this.meterRegistry = meterRegistry;
  }

  @Bean
  @Qualifier("nl2sqlChatClient")
  public ChatClient nl2sqlChatClient(
      ChatModel chatModel, PromptGuardAdvisor promptGuardAdvisor, VectorStore vectorStore) {
    QuestionAnswerAdvisor schemaRagAdvisor =
        QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().topK(8).build())
            .build();
    return ChatClient.builder(chatModel)
        .defaultSystem(new PromptTemplate(promptProperties.nl2sql()).render())
        .defaultAdvisors(
            promptGuardAdvisor, schemaRagAdvisor, new TokenUsageAdvisor(meterRegistry, "nl2sql"))
        .build();
  }

  @Bean
  @Qualifier("anomalyChatClient")
  public ChatClient anomalyChatClient(ChatModel chatModel, PromptGuardAdvisor promptGuardAdvisor) {
    return ChatClient.builder(chatModel)
        .defaultSystem(new PromptTemplate(promptProperties.anomalyExplain()).render())
        .defaultAdvisors(promptGuardAdvisor, new TokenUsageAdvisor(meterRegistry, "anomaly"))
        .build();
  }

  @Bean
  @Qualifier("reportChatClient")
  public ChatClient reportChatClient(ChatModel chatModel, PromptGuardAdvisor promptGuardAdvisor) {
    return ChatClient.builder(chatModel)
        .defaultSystem(new PromptTemplate(promptProperties.execReport()).render())
        .defaultAdvisors(promptGuardAdvisor, new TokenUsageAdvisor(meterRegistry, "report"))
        .build();
  }

  @Bean
  public PromptGuardAdvisor promptGuardAdvisor() {
    return new PromptGuardAdvisor();
  }
}
