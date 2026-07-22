package com.microsoft.financecopilot.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record QueryRequest(
    @NotBlank @Schema(example = "How much did I spend on groceries last month?")
        String naturalLanguageQuery) {}
