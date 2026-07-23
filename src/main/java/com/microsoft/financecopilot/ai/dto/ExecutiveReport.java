package com.microsoft.financecopilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/** Structured output shape for {@code reportChatClient}. */
public record ExecutiveReport(
    @JsonPropertyDescription("The full report rendered as Markdown") String markdown,
    @JsonPropertyDescription(
            "The report broken into its Summary/Key Metrics/Notable Anomalies/Outlook sections")
        List<ReportSection> sections) {}
