package com.microsoft.financecopilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/** Structured output shape for {@code nl2sqlChatClient}. Never regex a JSON blob for this. */
public record GeneratedSql(
    @JsonPropertyDescription("A single read-only PostgreSQL SELECT statement") String sql,
    @JsonPropertyDescription("Brief explanation of why this SQL answers the question")
        String rationale,
    @JsonPropertyDescription("Names of the analytics schema tables referenced")
        List<String> tablesUsed) {}
