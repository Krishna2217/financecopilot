package com.microsoft.financecopilot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Per-use-case model parameters for the three {@code ChatClient} beans. */
@Validated
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    @NotNull @Valid UseCase nl2sql,
    @NotNull @Valid UseCase anomaly,
    @NotNull @Valid UseCase report) {

  public record UseCase(
      @NotBlank String deploymentName,
      @DefaultValue("0.0") double temperature,
      @DefaultValue("2") int maxRetries,
      @DefaultValue("30s") java.time.Duration timeout) {}
}
