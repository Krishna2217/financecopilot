package com.microsoft.financecopilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Telemetry/observability settings; expanded further in Phase 7. */
@Validated
@ConfigurationProperties(prefix = "app.telemetry")
public record TelemetryProperties(
    @DefaultValue("financecopilot") String serviceName,
    @DefaultValue("true") boolean metricsEnabled) {}
