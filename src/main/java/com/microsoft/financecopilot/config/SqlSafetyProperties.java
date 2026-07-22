package com.microsoft.financecopilot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Connection details for the {@code finance_ro} read-only role, and the limits {@code
 * SqlSafetyValidator}/{@code SqlExecutor} enforce on AI-generated SQL.
 */
@Validated
@ConfigurationProperties(prefix = "app.sql-safety")
public record SqlSafetyProperties(
    @NotBlank String datasourceUrl,
    @NotBlank String username,
    @NotBlank String password,
    @Positive @DefaultValue("1000") int maxLimit,
    @DefaultValue("5s") Duration statementTimeout,
    @DefaultValue("analytics") String allowedSchema) {}
