package com.microsoft.financecopilot.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.config.SqlSafetyProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SchemaIntrospectorTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void cachesQualifiedAndUnqualifiedTableNamesFromTheAllowedSchema() {
    SqlSafetyProperties properties =
        new SqlSafetyProperties(
            "jdbc:postgresql://localhost:5432/financecopilot",
            "finance_ro",
            "changeme",
            1000,
            Duration.ofSeconds(5),
            "analytics");
    when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq("analytics")))
        .thenReturn(List.of("accounts", "transactions"));

    SchemaIntrospector introspector = new SchemaIntrospector(jdbcTemplate, properties);
    introspector.introspect();

    assertThat(introspector.allowedTables())
        .containsExactlyInAnyOrder(
            "accounts", "analytics.accounts", "transactions", "analytics.transactions");
  }

  @Test
  void startsWithAnEmptyAllowlistBeforeIntrospection() {
    SqlSafetyProperties properties =
        new SqlSafetyProperties(
            "jdbc:postgresql://localhost:5432/financecopilot",
            "finance_ro",
            "changeme",
            1000,
            Duration.ofSeconds(5),
            "analytics");
    SchemaIntrospector introspector = new SchemaIntrospector(jdbcTemplate, properties);

    assertThat(introspector.allowedTables()).isEmpty();
  }
}
