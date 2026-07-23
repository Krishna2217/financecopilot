package com.microsoft.financecopilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Structured output shape for {@code anomalyChatClient}. */
public record AnomalyExplanation(
    @JsonPropertyDescription("A 2-4 sentence, plain-language explanation of the spending anomaly")
        String explanation) {}
