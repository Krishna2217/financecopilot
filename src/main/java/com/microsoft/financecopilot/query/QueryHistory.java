package com.microsoft.financecopilot.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "query_history", schema = "app")
public class QueryHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nl_query", nullable = false)
  private String nlQuery;

  @Column(name = "generated_sql", nullable = false)
  private String generatedSql;

  private String rationale;

  @Column(name = "tables_used")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] tablesUsed;

  @Column(name = "rows_returned")
  private Integer rowsReturned;

  private String summary;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "completion_tokens")
  private Integer completionTokens;

  @Column(name = "sql_exec_ms")
  private Long sqlExecMs;

  @Column(name = "total_ms")
  private Long totalMs;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public String getNlQuery() {
    return nlQuery;
  }

  public void setNlQuery(String nlQuery) {
    this.nlQuery = nlQuery;
  }

  public String getGeneratedSql() {
    return generatedSql;
  }

  public void setGeneratedSql(String generatedSql) {
    this.generatedSql = generatedSql;
  }

  public String getRationale() {
    return rationale;
  }

  public void setRationale(String rationale) {
    this.rationale = rationale;
  }

  public String[] getTablesUsed() {
    return tablesUsed;
  }

  public void setTablesUsed(String[] tablesUsed) {
    this.tablesUsed = tablesUsed;
  }

  public Integer getRowsReturned() {
    return rowsReturned;
  }

  public void setRowsReturned(Integer rowsReturned) {
    this.rowsReturned = rowsReturned;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public Integer getPromptTokens() {
    return promptTokens;
  }

  public void setPromptTokens(Integer promptTokens) {
    this.promptTokens = promptTokens;
  }

  public Integer getCompletionTokens() {
    return completionTokens;
  }

  public void setCompletionTokens(Integer completionTokens) {
    this.completionTokens = completionTokens;
  }

  public Long getSqlExecMs() {
    return sqlExecMs;
  }

  public void setSqlExecMs(Long sqlExecMs) {
    this.sqlExecMs = sqlExecMs;
  }

  public Long getTotalMs() {
    return totalMs;
  }

  public void setTotalMs(Long totalMs) {
    this.totalMs = totalMs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
