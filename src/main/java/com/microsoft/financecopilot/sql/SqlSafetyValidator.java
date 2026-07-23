package com.microsoft.financecopilot.sql;

import com.microsoft.financecopilot.common.exception.SqlSafetyViolationException;
import com.microsoft.financecopilot.config.SqlSafetyProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

/**
 * The authoritative safety gate between model-generated SQL and execution. Deliberately not an
 * advisor: it runs in the service layer, between {@code ChatClient} output and {@link SqlExecutor},
 * per CLAUDE.md's SQL safety rules.
 *
 * <ol>
 *   <li>Parse with JSqlParser; reject unless exactly one statement, and that statement is a {@code
 *       SELECT}. Using a real parser (rather than splitting on {@code ;}) means a malicious second
 *       statement can't be smuggled past the check by hiding it near a comment.
 *   <li>Reject any DDL/DML/COPY/CALL/GRANT/REVOKE/TRUNCATE token (and a handful of dangerous
 *       functions) as a defense-in-depth backstop over the structural check above.
 *   <li>Enforce the table allow-list from {@link SchemaIntrospector}.
 *   <li>Enforce {@code LIMIT <= maxLimit} (injecting one if absent, clamping one that's too high).
 * </ol>
 */
@Component
public class SqlSafetyValidator {

  private static final Set<String> FORBIDDEN_TOKENS =
      Set.of(
          "DROP",
          "DELETE",
          "UPDATE",
          "INSERT",
          "ALTER",
          "TRUNCATE",
          "GRANT",
          "REVOKE",
          "CREATE",
          "EXEC",
          "EXECUTE",
          "CALL",
          "COPY",
          "MERGE",
          "VACUUM",
          "REINDEX",
          "LISTEN",
          "NOTIFY",
          "PG_SLEEP",
          "DBLINK",
          "LO_IMPORT",
          "LO_EXPORT",
          "PG_READ_FILE",
          "PG_READ_BINARY_FILE");

  private final SchemaIntrospector schemaIntrospector;
  private final SqlSafetyProperties properties;
  private final MeterRegistry meterRegistry;

  public SqlSafetyValidator(
      SchemaIntrospector schemaIntrospector,
      SqlSafetyProperties properties,
      MeterRegistry meterRegistry) {
    this.schemaIntrospector = schemaIntrospector;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  /** Returns the (possibly limit-rewritten) SQL text that is safe to execute. */
  public String validate(String rawSql) {
    try {
      return doValidate(rawSql);
    } catch (SqlSafetyViolationException e) {
      meterRegistry.counter("sql.rejected.count").increment();
      throw e;
    }
  }

  private String doValidate(String rawSql) {
    if (rawSql == null || rawSql.isBlank()) {
      throw new SqlSafetyViolationException("SQL must not be blank");
    }

    Statements statements;
    try {
      statements = CCJSqlParserUtil.parseStatements(rawSql);
    } catch (JSQLParserException e) {
      throw new SqlSafetyViolationException("Unable to parse SQL: " + e.getMessage());
    }

    List<Statement> parsed = statements.getStatements();
    if (parsed.size() != 1) {
      throw new SqlSafetyViolationException(
          "Exactly one SQL statement is required, found " + parsed.size());
    }

    Statement statement = parsed.get(0);
    if (!(statement instanceof Select select)) {
      throw new SqlSafetyViolationException(
          "Only SELECT statements are allowed, found: " + statement.getClass().getSimpleName());
    }

    // Scan the parser's own re-serialization rather than the raw input: it has no comments, so a
    // forbidden word sitting inside a comment (as opposed to inside real SQL) can't false-positive
    // here. Comments can't hide a *second* statement either way, since the statement-count check
    // above operates on the parsed AST, not raw text.
    String upperSql = select.toString().toUpperCase(Locale.ROOT);
    for (String token : FORBIDDEN_TOKENS) {
      if (containsWord(upperSql, token)) {
        throw new SqlSafetyViolationException("Statement contains forbidden token: " + token);
      }
    }

    Set<String> referencedTables = new TablesNamesFinder().getTables(statement);
    Set<String> allowedTables = schemaIntrospector.allowedTables();
    for (String table : referencedTables) {
      if (!allowedTables.contains(table.toLowerCase(Locale.ROOT))) {
        throw new SqlSafetyViolationException("Table not permitted: " + table);
      }
    }

    enforceLimit(select);
    return select.toString();
  }

  private void enforceLimit(Select select) {
    Limit limit = select.getLimit();
    if (limit == null) {
      Limit injected = new Limit();
      injected.setRowCount(new LongValue(properties.maxLimit()));
      select.setLimit(injected);
      return;
    }
    Expression rowCount = limit.getRowCount();
    if (rowCount instanceof LongValue longValue && longValue.getValue() > properties.maxLimit()) {
      limit.setRowCount(new LongValue(properties.maxLimit()));
    }
  }

  private boolean containsWord(String haystack, String word) {
    return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(haystack).find();
  }
}
