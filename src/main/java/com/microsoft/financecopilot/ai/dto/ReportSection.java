package com.microsoft.financecopilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReportSection(
    @JsonPropertyDescription(
            "Section heading, e.g. Summary, Key Metrics, Notable Anomalies, Outlook")
        String title,
    @JsonPropertyDescription("Markdown content of this section") String content) {}
