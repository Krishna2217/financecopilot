package com.microsoft.financecopilot.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    example = "{\"category\": \"Groceries\", \"totalAmount\": 412.35, \"transactionCount\": 18}")
public record CategorySpend(String category, BigDecimal totalAmount, long transactionCount) {}
