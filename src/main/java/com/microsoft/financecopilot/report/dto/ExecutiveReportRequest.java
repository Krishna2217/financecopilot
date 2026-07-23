package com.microsoft.financecopilot.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExecutiveReportRequest(
    @NotNull @Min(2000) @Max(2100) @Schema(example = "2026") Integer year,
    @NotNull @Min(1) @Max(12) @Schema(example = "6") Integer month) {}
