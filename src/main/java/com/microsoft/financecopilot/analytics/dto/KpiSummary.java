package com.microsoft.financecopilot.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(
    example =
        """
        {
          "month": "2026-06",
          "totalIncome": 5200.00,
          "totalExpenses": 3120.45,
          "netCashflow": 2079.55,
          "savingsRatePercent": 39.99
        }
        """)
public record KpiSummary(
    YearMonth month,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netCashflow,
    BigDecimal savingsRatePercent) {}
