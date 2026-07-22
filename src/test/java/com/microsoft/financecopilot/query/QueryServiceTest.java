package com.microsoft.financecopilot.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.ai.dto.GeneratedSql;
import com.microsoft.financecopilot.common.exception.SqlExecutionException;
import com.microsoft.financecopilot.common.exception.SqlSafetyViolationException;
import com.microsoft.financecopilot.query.dto.QueryResponse;
import com.microsoft.financecopilot.sql.QueryResult;
import com.microsoft.financecopilot.sql.SqlExecutor;
import com.microsoft.financecopilot.sql.SqlSafetyValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @Mock private SqlSafetyValidator sqlSafetyValidator;
  @Mock private SqlExecutor sqlExecutor;
  @Mock private QueryHistoryRepository queryHistoryRepository;

  private ChatClient nl2sqlChatClient;
  private ChatClient.ChatClientRequestSpec nl2sqlRequestSpec;
  private ChatClient.CallResponseSpec nl2sqlCallSpec;

  private ChatClient reportChatClient;
  private ChatClient.ChatClientRequestSpec reportRequestSpec;
  private ChatClient.CallResponseSpec reportCallSpec;

  private QueryService queryService;

  @BeforeEach
  void setUp() {
    nl2sqlChatClient = mock(ChatClient.class);
    nl2sqlRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    nl2sqlCallSpec = mock(ChatClient.CallResponseSpec.class);
    when(nl2sqlChatClient.prompt()).thenReturn(nl2sqlRequestSpec);
    when(nl2sqlRequestSpec.user(anyString())).thenReturn(nl2sqlRequestSpec);
    when(nl2sqlRequestSpec.call()).thenReturn(nl2sqlCallSpec);

    reportChatClient = mock(ChatClient.class);
    reportRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    reportCallSpec = mock(ChatClient.CallResponseSpec.class);

    queryService =
        new QueryService(
            nl2sqlChatClient,
            reportChatClient,
            sqlSafetyValidator,
            sqlExecutor,
            queryHistoryRepository);
  }

  @Test
  void happyPathExecutesValidatesSummarizesAndPersistsHistory() {
    GeneratedSql generatedSql =
        new GeneratedSql(
            "SELECT sum(amount) FROM analytics.transactions",
            "Sums all transaction amounts",
            List.of("transactions"));
    ChatResponse nl2sqlChatResponse =
        new ChatResponse(
            List.of(new Generation(new AssistantMessage("irrelevant"))),
            ChatResponseMetadata.builder().usage(new DefaultUsage(100, 20)).build());
    when(nl2sqlCallSpec.responseEntity(GeneratedSql.class))
        .thenReturn(new ResponseEntity<>(nl2sqlChatResponse, generatedSql));

    when(sqlSafetyValidator.validate(generatedSql.sql()))
        .thenReturn("SELECT sum(amount) FROM analytics.transactions LIMIT 1000");
    QueryResult queryResult = new QueryResult(List.of("sum"), List.of(Map.of("sum", -312.47)));
    when(sqlExecutor.execute("SELECT sum(amount) FROM analytics.transactions LIMIT 1000"))
        .thenReturn(queryResult);

    when(reportChatClient.prompt()).thenReturn(reportRequestSpec);
    when(reportRequestSpec.user(anyString())).thenReturn(reportRequestSpec);
    when(reportRequestSpec.call()).thenReturn(reportCallSpec);
    when(reportCallSpec.content()).thenReturn("You spent $312.47 last month.");

    when(queryHistoryRepository.save(any(QueryHistory.class)))
        .thenAnswer(
            invocation -> {
              QueryHistory saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", 42L);
              return saved;
            });

    QueryResponse response = queryService.executeQuery("How much did I spend last month?");

    assertThat(response.generatedSql())
        .isEqualTo("SELECT sum(amount) FROM analytics.transactions LIMIT 1000");
    assertThat(response.rationale()).isEqualTo("Sums all transaction amounts");
    assertThat(response.columns()).containsExactly("sum");
    assertThat(response.rows()).containsExactly(Map.of("sum", -312.47));
    assertThat(response.summary()).isEqualTo("You spent $312.47 last month.");
    assertThat(response.historyId()).isEqualTo(42L);

    var historyCaptor = org.mockito.ArgumentCaptor.forClass(QueryHistory.class);
    verify(queryHistoryRepository).save(historyCaptor.capture());
    QueryHistory persisted = historyCaptor.getValue();
    assertThat(persisted.getNlQuery()).isEqualTo("How much did I spend last month?");
    assertThat(persisted.getPromptTokens()).isEqualTo(100);
    assertThat(persisted.getCompletionTokens()).isEqualTo(20);
    assertThat(persisted.getRowsReturned()).isEqualTo(1);
    assertThat(persisted.getTablesUsed()).containsExactly("transactions");
  }

  @Test
  void unsafeSqlPropagatesAndNeverExecutesOrPersists() {
    GeneratedSql generatedSql =
        new GeneratedSql("DROP TABLE analytics.accounts", "malicious", List.of());
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(""))));
    when(nl2sqlCallSpec.responseEntity(GeneratedSql.class))
        .thenReturn(new ResponseEntity<>(chatResponse, generatedSql));
    when(sqlSafetyValidator.validate(generatedSql.sql()))
        .thenThrow(new SqlSafetyViolationException("Statement contains forbidden token: DROP"));

    assertThatThrownBy(() -> queryService.executeQuery("drop my accounts table"))
        .isInstanceOf(SqlSafetyViolationException.class);

    verify(sqlExecutor, never()).execute(anyString());
    verify(queryHistoryRepository, never()).save(any());
  }

  @Test
  void sqlExecutionTimeoutPropagatesAndNeverPersists() {
    GeneratedSql generatedSql =
        new GeneratedSql(
            "SELECT * FROM analytics.transactions", "rationale", List.of("transactions"));
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(""))));
    when(nl2sqlCallSpec.responseEntity(GeneratedSql.class))
        .thenReturn(new ResponseEntity<>(chatResponse, generatedSql));
    when(sqlSafetyValidator.validate(generatedSql.sql())).thenReturn(generatedSql.sql());
    when(sqlExecutor.execute(generatedSql.sql()))
        .thenThrow(
            new SqlExecutionException(
                "Failed to execute SQL: canceling statement due to statement timeout"));

    assertThatThrownBy(() -> queryService.executeQuery("show me everything"))
        .isInstanceOf(SqlExecutionException.class);

    verify(queryHistoryRepository, never()).save(any());
  }

  @Test
  void upstreamRateLimitPropagatesBeforeValidationOrExecution() {
    when(nl2sqlCallSpec.responseEntity(GeneratedSql.class))
        .thenThrow(new TransientAiException("HTTP 429 Too Many Requests"));

    assertThatThrownBy(() -> queryService.executeQuery("how much did I spend"))
        .isInstanceOf(TransientAiException.class);

    verify(sqlSafetyValidator, never()).validate(anyString());
    verify(sqlExecutor, never()).execute(anyString());
    verify(queryHistoryRepository, never()).save(any());
  }
}
