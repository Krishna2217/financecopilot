package com.microsoft.financecopilot.sql;

import com.microsoft.financecopilot.config.SqlSafetyProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A dedicated, small connection pool authenticated as {@code finance_ro} — kept separate from the
 * app's main (finance_app) datasource so a bug in application code can never reuse finance_app's
 * write privileges to run AI-generated SQL, or vice versa.
 */
@Configuration
public class SqlDataSourceConfig {

  @Bean
  @Qualifier("financeReadOnlyDataSource")
  public DataSource financeReadOnlyDataSource(SqlSafetyProperties properties) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.datasourceUrl());
    config.setUsername(properties.username());
    config.setPassword(properties.password());
    config.setReadOnly(true);
    config.setMaximumPoolSize(5);
    config.setPoolName("finance-ro-pool");
    return new HikariDataSource(config);
  }
}
