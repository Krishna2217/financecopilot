package com.microsoft.financecopilot.anomaly.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(
    example =
        """
        {
          "id": 7,
          "categoryId": 9,
          "categoryName": "Entertainment",
          "month": "2026-06",
          "spend": 612.40,
          "meanSpend": 210.15,
          "zScore": 3.82,
          "iqrFlag": true,
          "explanation": null
        }
        """)
public record AnomalyResponse(
    Long id,
    Long categoryId,
    String categoryName,
    YearMonth month,
    BigDecimal spend,
    BigDecimal meanSpend,
    BigDecimal zScore,
    boolean iqrFlag,
    String explanation) {}
