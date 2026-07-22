package com.microsoft.financecopilot.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(example = "{\"month\": \"2026-06\", \"totalAmount\": 3120.45}")
public record TrendPoint(YearMonth month, BigDecimal totalAmount) {}
