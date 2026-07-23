package com.microsoft.financecopilot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.ai.dto.AnomalyExplanation;
import com.microsoft.financecopilot.ai.dto.ExecutiveReport;
import com.microsoft.financecopilot.ai.dto.GeneratedSql;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Verifies the actual structured-output mechanism (Spring AI's {@code .entity(Class)} JSON
 * conversion) end to end against a mocked {@link ChatModel} returning a canned response, for each
 * of the three DTOs the project relies on. This exercises the real converter rather than assuming —
 * elsewhere the {@code ChatClient} itself is mocked directly, which never touches this parsing
 * path.
 */
class StructuredOutputIntegrationTest {

  @Test
  void parsesACannedGeneratedSqlResponse() {
    ChatClient chatClient =
        chatClientReturning(
            """
        {
          "sql": "SELECT sum(amount) FROM analytics.transactions LIMIT 1000",
          "rationale": "Sums all transaction amounts",
          "tablesUsed": ["transactions"]
        }
        """);

    GeneratedSql result =
        chatClient.prompt().user("How much did I spend?").call().entity(GeneratedSql.class);

    assertThat(result.sql()).isEqualTo("SELECT sum(amount) FROM analytics.transactions LIMIT 1000");
    assertThat(result.rationale()).isEqualTo("Sums all transaction amounts");
    assertThat(result.tablesUsed()).containsExactly("transactions");
  }

  @Test
  void parsesACannedAnomalyExplanationResponse() {
    ChatClient chatClient =
        chatClientReturning(
            """
            {
              "explanation": "Your utilities spend doubled compared to your usual average."
            }
            """);

    AnomalyExplanation result =
        chatClient.prompt().user("Explain this anomaly").call().entity(AnomalyExplanation.class);

    assertThat(result.explanation())
        .isEqualTo("Your utilities spend doubled compared to your usual average.");
  }

  @Test
  void parsesACannedExecutiveReportResponse() {
    ChatClient chatClient =
        chatClientReturning(
            """
            {
              "markdown": "# Executive Summary\\n\\nAll good.",
              "sections": [
                {"title": "Summary", "content": "All good."}
              ]
            }
            """);

    ExecutiveReport result =
        chatClient.prompt().user("Generate the report").call().entity(ExecutiveReport.class);

    assertThat(result.markdown()).isEqualTo("# Executive Summary\n\nAll good.");
    assertThat(result.sections()).hasSize(1);
    assertThat(result.sections().get(0).title()).isEqualTo("Summary");
    assertThat(result.sections().get(0).content()).isEqualTo("All good.");
  }

  private ChatClient chatClientReturning(String cannedJson) {
    ChatModel chatModel = mock(ChatModel.class);
    ChatResponse cannedResponse =
        new ChatResponse(List.of(new Generation(new AssistantMessage(cannedJson))));
    when(chatModel.call(ArgumentMatchers.any(Prompt.class))).thenReturn(cannedResponse);
    return ChatClient.builder(chatModel).build();
  }
}
