package com.microsoft.financecopilot.anomaly.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Raw statistical output of {@code AnomalyDetector}, before any persistence or explanation. */
public record DetectedAnomaly(
    Long categoryId,
    String categoryName,
    YearMonth month,
    BigDecimal spend,
    BigDecimal meanSpend,
    BigDecimal zScore,
    boolean iqrFlag) {}
