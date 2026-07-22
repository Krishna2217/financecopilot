package com.microsoft.financecopilot.query;

import com.microsoft.financecopilot.ai.dto.GeneratedSql;
import com.microsoft.financecopilot.query.dto.QueryHistoryItem;
import com.microsoft.financecopilot.query.dto.QueryResponse;
import com.microsoft.financecopilot.sql.QueryResult;
import com.microsoft.financecopilot.sql.SqlExecutor;
import com.microsoft.financecopilot.sql.SqlSafetyValidator;
import java.time.Instant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the NL→SQL flow: {@code nl2sqlChatClient} → {@link SqlSafetyValidator} → {@link
 * SqlExecutor} → a short summary from {@code reportChatClient} → persisted {@link QueryHistory}.
 *
 * <p>Gated by {@code app.ai.enabled}, like {@code ChatClientConfig}: this depends directly on the
 * {@code nl2sqlChatClient}/{@code reportChatClient} beans, which don't exist locally without
 * Azure/OpenAI credentials.
 */
@Service
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class QueryService {

  private final ChatClient nl2sqlChatClient;
  private final ChatClient reportChatClient;
  private final SqlSafetyValidator sqlSafetyValidator;
  private final SqlExecutor sqlExecutor;
  private final QueryHistoryRepository queryHistoryRepository;

  public QueryService(
      @Qualifier("nl2sqlChatClient") ChatClient nl2sqlChatClient,
      @Qualifier("reportChatClient") ChatClient reportChatClient,
      SqlSafetyValidator sqlSafetyValidator,
      SqlExecutor sqlExecutor,
      QueryHistoryRepository queryHistoryRepository) {
    this.nl2sqlChatClient = nl2sqlChatClient;
    this.reportChatClient = reportChatClient;
    this.sqlSafetyValidator = sqlSafetyValidator;
    this.sqlExecutor = sqlExecutor;
    this.queryHistoryRepository = queryHistoryRepository;
  }

  public QueryResponse executeQuery(String naturalLanguageQuery) {
    long startMs = System.currentTimeMillis();

    ResponseEntity<ChatResponse, GeneratedSql> nl2sqlResponse =
        nl2sqlChatClient
            .prompt()
            .user(naturalLanguageQuery)
            .call()
            .responseEntity(GeneratedSql.class);
    GeneratedSql generatedSql = nl2sqlResponse.entity();

    String validatedSql = sqlSafetyValidator.validate(generatedSql.sql());

    long sqlStartMs = System.currentTimeMillis();
    QueryResult result = sqlExecutor.execute(validatedSql);
    long sqlExecMs = System.currentTimeMillis() - sqlStartMs;

    String summary =
        reportChatClient
            .prompt()
            .user(summaryPrompt(naturalLanguageQuery, result))
            .call()
            .content();

    long totalMs = System.currentTimeMillis() - startMs;
    Usage usage = nl2sqlResponse.response().getMetadata().getUsage();

    QueryHistory history = new QueryHistory();
    history.setNlQuery(naturalLanguageQuery);
    history.setGeneratedSql(validatedSql);
    history.setRationale(generatedSql.rationale());
    history.setTablesUsed(
        generatedSql.tablesUsed() == null
            ? new String[0]
            : generatedSql.tablesUsed().toArray(new String[0]));
    history.setRowsReturned(result.rows().size());
    history.setSummary(summary);
    history.setPromptTokens(usage != null ? usage.getPromptTokens() : null);
    history.setCompletionTokens(usage != null ? usage.getCompletionTokens() : null);
    history.setSqlExecMs(sqlExecMs);
    history.setTotalMs(totalMs);
    history.setCreatedAt(Instant.now());
    QueryHistory saved = queryHistoryRepository.save(history);

    return new QueryResponse(
        validatedSql,
        generatedSql.rationale(),
        result.columns(),
        result.rows(),
        summary,
        saved.getId());
  }

  public Page<QueryHistoryItem> getHistory(Pageable pageable) {
    return queryHistoryRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toItem);
  }

  private String summaryPrompt(String naturalLanguageQuery, QueryResult result) {
    return "Write a 2-sentence summary of these query results for a finance user.\n"
        + "Question: "
        + naturalLanguageQuery
        + "\nColumns: "
        + result.columns()
        + "\nRows: "
        + result.rows();
  }

  private QueryHistoryItem toItem(QueryHistory history) {
    return new QueryHistoryItem(
        history.getId(),
        history.getNlQuery(),
        history.getGeneratedSql(),
        history.getRationale(),
        history.getRowsReturned(),
        history.getSummary(),
        history.getPromptTokens(),
        history.getCompletionTokens(),
        history.getSqlExecMs(),
        history.getTotalMs(),
        history.getCreatedAt());
  }
}
