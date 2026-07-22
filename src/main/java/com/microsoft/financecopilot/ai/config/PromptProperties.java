package com.microsoft.financecopilot.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/** Classpath locations of the system prompt template for each {@code ChatClient} use case. */
@Validated
@ConfigurationProperties(prefix = "app.ai.prompts")
public record PromptProperties(
    @DefaultValue("classpath:/prompts/nl2sql.st") Resource nl2sql,
    @DefaultValue("classpath:/prompts/anomaly-explain.st") Resource anomalyExplain,
    @DefaultValue("classpath:/prompts/exec-report.st") Resource execReport) {}
