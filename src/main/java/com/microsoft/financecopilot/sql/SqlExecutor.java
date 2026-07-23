package com.microsoft.financecopilot.sql;

import com.microsoft.financecopilot.common.exception.SqlExecutionException;
import com.microsoft.financecopilot.config.SqlSafetyProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Executes already-{@link SqlSafetyValidator}-validated SQL against the {@code finance_ro} role.
 * {@code SET LOCAL statement_timeout} is issued in the same transaction as the query (autocommit
 * disabled for the duration) since {@code SET LOCAL} only lasts until the transaction ends.
 */
@Component
public class SqlExecutor {

  private final DataSource financeReadOnlyDataSource;
  private final SqlSafetyProperties properties;
  private final MeterRegistry meterRegistry;

  public SqlExecutor(
      @Qualifier("financeReadOnlyDataSource") DataSource financeReadOnlyDataSource,
      SqlSafetyProperties properties,
      MeterRegistry meterRegistry) {
    this.financeReadOnlyDataSource = financeReadOnlyDataSource;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  public QueryResult execute(String validatedSql) {
    long startNanos = System.nanoTime();
    try (Connection connection = financeReadOnlyDataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement.execute(
            "SET LOCAL statement_timeout = '" + properties.statementTimeout().toMillis() + "ms'");
        try (ResultSet resultSet = statement.executeQuery(validatedSql)) {
          QueryResult result = toQueryResult(resultSet);
          connection.commit();
          return result;
        }
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new SqlExecutionException("Failed to execute SQL: " + e.getMessage());
    } finally {
      meterRegistry
          .timer("sql.exec.ms")
          .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
  }

  private QueryResult toQueryResult(ResultSet resultSet) throws SQLException {
    ResultSetMetaData metadata = resultSet.getMetaData();
    int columnCount = metadata.getColumnCount();
    List<String> columns = new ArrayList<>(columnCount);
    for (int i = 1; i <= columnCount; i++) {
      columns.add(metadata.getColumnLabel(i));
    }

    List<Map<String, Object>> rows = new ArrayList<>();
    while (resultSet.next()) {
      Map<String, Object> row = new LinkedHashMap<>(columnCount);
      for (int i = 1; i <= columnCount; i++) {
        row.put(columns.get(i - 1), resultSet.getObject(i));
      }
      rows.add(row);
    }
    return new QueryResult(columns, rows);
  }
}
