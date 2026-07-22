package com.microsoft.financecopilot.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.common.exception.SqlExecutionException;
import com.microsoft.financecopilot.config.SqlSafetyProperties;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqlExecutorTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private Statement statement;
  @Mock private ResultSet resultSet;
  @Mock private ResultSetMetaData resultSetMetaData;

  private SqlExecutor executor;

  @BeforeEach
  void setUp() throws SQLException {
    SqlSafetyProperties properties =
        new SqlSafetyProperties(
            "jdbc:postgresql://localhost:5432/financecopilot",
            "finance_ro",
            "changeme",
            1000,
            Duration.ofSeconds(5),
            "analytics");
    executor = new SqlExecutor(dataSource, properties);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
  }

  @Test
  void setsStatementTimeoutWithinTheSameTransactionAsTheQuery() throws SQLException {
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSetMetaData.getColumnCount()).thenReturn(1);
    when(resultSetMetaData.getColumnLabel(1)).thenReturn("name");
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getObject(1)).thenReturn("Groceries");

    QueryResult result = executor.execute("SELECT name FROM analytics.categories LIMIT 1");

    verify(connection).setAutoCommit(false);
    verify(statement).execute(contains("SET LOCAL statement_timeout"));
    verify(connection).commit();
    assertThat(result.columns()).containsExactly("name");
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().get(0)).containsEntry("name", "Groceries");
  }

  @Test
  void rollsBackAndWrapsExceptionWhenQueryFails() throws SQLException {
    doThrow(new SQLException("timeout")).when(statement).executeQuery(anyString());

    assertThatThrownBy(() -> executor.execute("SELECT * FROM analytics.transactions"))
        .isInstanceOf(SqlExecutionException.class)
        .hasMessageContaining("Failed to execute SQL");

    verify(connection).rollback();
  }
}
