package com.microsoft.financecopilot.sql;

import com.microsoft.financecopilot.config.SqlSafetyProperties;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches the set of tables in the allow-listed schema (see {@link
 * SqlSafetyProperties#allowedSchema()}) in memory on startup, so {@link SqlSafetyValidator} can
 * reject AI-generated SQL that references any table outside it without a schema round-trip per
 * query. Re-ingested into pgvector for retrieval in Phase 2.5; this is the authoritative allowlist
 * used for validation.
 */
@Component
public class SchemaIntrospector {

  private final JdbcTemplate jdbcTemplate;
  private final SqlSafetyProperties properties;
  private volatile Set<String> allowedTables = Set.of();

  public SchemaIntrospector(JdbcTemplate jdbcTemplate, SqlSafetyProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void introspect() {
    List<String> tableNames =
        jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = ?",
            String.class,
            properties.allowedSchema());

    Set<String> tables = new HashSet<>();
    for (String tableName : tableNames) {
      String lower = tableName.toLowerCase(Locale.ROOT);
      tables.add(lower);
      tables.add(properties.allowedSchema().toLowerCase(Locale.ROOT) + "." + lower);
    }
    this.allowedTables = Set.copyOf(tables);
  }

  /** Both unqualified (e.g. {@code transactions}) and schema-qualified names are included. */
  public Set<String> allowedTables() {
    return allowedTables;
  }
}
