package com.microsoft.financecopilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI financeCopilotOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FinanceCopilot API")
                .description(
                    "AI Finance Analytics Platform — NL→SQL, anomaly detection, executive reporting.")
                .version("v1"));
  }
}
