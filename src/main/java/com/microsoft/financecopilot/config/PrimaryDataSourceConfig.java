package com.microsoft.financecopilot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.constraints.NotBlank;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

/**
 * Explicitly declares the application's main (finance_app) datasource, marked {@code @Primary}.
 *
 * <p>Without this, {@code SqlDataSourceConfig}'s {@code financeReadOnlyDataSource} bean
 * (finance_ro) satisfies Spring Boot's {@code @ConditionalOnMissingBean(DataSource.class)} for its
 * own auto-configured datasource — by type, not qualifier — so Boot silently skips creating the
 * real application datasource, leaving finance_ro as the only {@code DataSource} bean in the
 * context. JPA/JdbcTemplate then silently run everything (including app.* writes) through
 * finance_ro, which has no grants there at all.
 */
@Configuration
public class PrimaryDataSourceConfig {

  @Bean
  @Primary
  public DataSource dataSource(ApplicationDataSourceProperties properties) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.url());
    config.setUsername(properties.username());
    config.setPassword(properties.password());
    config.setPoolName("finance-app-pool");
    return new HikariDataSource(config);
  }

  @Validated
  @ConfigurationProperties(prefix = "spring.datasource")
  public record ApplicationDataSourceProperties(
      @NotBlank String url, @NotBlank String username, @NotBlank String password) {}
}
