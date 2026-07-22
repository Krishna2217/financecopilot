package com.microsoft.financecopilot.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(
    example =
        "{\"month\": \"2026-06\", \"income\": 5200.00, \"expenses\": 3120.45, \"netCashflow\": 2079.55}")
public record CashflowPoint(
    YearMonth month, BigDecimal income, BigDecimal expenses, BigDecimal netCashflow) {}
