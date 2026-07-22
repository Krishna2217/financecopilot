package com.microsoft.financecopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinanceCopilotApplication {

  public static void main(String[] args) {
    SpringApplication.run(FinanceCopilotApplication.class, args);
  }
}
