package com.microsoft.financecopilot.query.dto;

import java.time.Instant;

public record QueryHistoryItem(
    Long id,
    String nlQuery,
    String generatedSql,
    String rationale,
    Integer rowsReturned,
    String summary,
    Integer promptTokens,
    Integer completionTokens,
    Long sqlExecMs,
    Long totalMs,
    Instant createdAt) {}
