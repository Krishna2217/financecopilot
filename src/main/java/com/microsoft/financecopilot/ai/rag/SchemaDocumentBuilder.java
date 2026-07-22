package com.microsoft.financecopilot.ai.rag;

import com.microsoft.financecopilot.config.SqlSafetyProperties;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Renders one retrievable {@link Document} per table in the allow-listed ({@code analytics})
 * schema: columns + types, the foreign-key graph, and a few sample rows, all introspected live from
 * {@code information_schema} rather than hand-maintained, so the RAG context never drifts from the
 * real schema.
 */
@Component
public class SchemaDocumentBuilder {

  private static final Map<String, String> TABLE_DESCRIPTIONS =
      Map.of(
          "accounts", "Bank, credit card, and investment accounts the user holds.",
          "categories",
              "Spending and income categories, optionally nested under a parent category.",
          "transactions",
              "Individual financial transactions (income and expenses) tied to an account and a "
                  + "category. Use this table to compute spend, income, or cashflow over a date "
                  + "range such as last month.",
          "budgets", "Monthly spending limits set per category.",
          "monthly_summary",
              "Pre-aggregated monthly totals and transaction counts per account and category.");

  private final JdbcTemplate jdbcTemplate;
  private final SqlSafetyProperties properties;

  public SchemaDocumentBuilder(JdbcTemplate jdbcTemplate, SqlSafetyProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  public List<Document> buildTableDocuments() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? ORDER BY table_name",
            String.class,
            properties.allowedSchema());
    return tables.stream().map(this::buildDocumentForTable).toList();
  }

  private Document buildDocumentForTable(String table) {
    List<Map<String, Object>> columns =
        jdbcTemplate.queryForList(
            "SELECT column_name, data_type FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
            properties.allowedSchema(),
            table);

    List<Map<String, Object>> foreignKeys =
        jdbcTemplate.queryForList(
            "SELECT kcu.column_name AS column_name, ccu.table_name AS foreign_table, "
                + "ccu.column_name AS foreign_column "
                + "FROM information_schema.table_constraints tc "
                + "JOIN information_schema.key_column_usage kcu "
                + "  ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema "
                + "JOIN information_schema.constraint_column_usage ccu "
                + "  ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema "
                + "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = ? AND tc.table_name = ?",
            properties.allowedSchema(),
            table);

    // `table` always comes from information_schema (introspected above), never from user input,
    // so string-concatenating it into this SELECT carries no injection risk.
    List<Map<String, Object>> sampleRows =
        jdbcTemplate.queryForList(
            "SELECT * FROM " + properties.allowedSchema() + "." + table + " LIMIT 3");

    String content = render(table, columns, foreignKeys, sampleRows);
    return new Document(
        RagDocumentIds.deterministicId("table:" + table),
        content,
        Map.of("type", "schema-table", "table", table));
  }

  private String render(
      String table,
      List<Map<String, Object>> columns,
      List<Map<String, Object>> foreignKeys,
      List<Map<String, Object>> sampleRows) {
    StringBuilder sb = new StringBuilder();
    sb.append("Table: ").append(properties.allowedSchema()).append('.').append(table).append('\n');
    sb.append("Description: ")
        .append(TABLE_DESCRIPTIONS.getOrDefault(table, "No description available."))
        .append('\n');

    sb.append("Columns: ")
        .append(
            columns.stream()
                .map(c -> c.get("column_name") + " (" + c.get("data_type") + ")")
                .collect(Collectors.joining(", ")))
        .append('\n');

    if (!foreignKeys.isEmpty()) {
      sb.append("Foreign keys: ")
          .append(
              foreignKeys.stream()
                  .map(
                      fk ->
                          fk.get("column_name")
                              + " -> "
                              + properties.allowedSchema()
                              + "."
                              + fk.get("foreign_table")
                              + "."
                              + fk.get("foreign_column"))
                  .collect(Collectors.joining(", ")))
          .append('\n');
    }

    if (!sampleRows.isEmpty()) {
      sb.append("Sample rows:\n");
      for (Map<String, Object> row : sampleRows) {
        sb.append("  ").append(row).append('\n');
      }
    }

    return sb.toString();
  }
}
