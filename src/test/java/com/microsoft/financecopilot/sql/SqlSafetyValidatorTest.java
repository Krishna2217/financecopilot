package com.microsoft.financecopilot.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import com.microsoft.financecopilot.common.exception.SqlSafetyViolationException;
import com.microsoft.financecopilot.config.SqlSafetyProperties;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqlSafetyValidatorTest {

  @Mock private SchemaIntrospector schemaIntrospector;

  private SqlSafetyValidator validator;

  @BeforeEach
  void setUp() {
    lenient()
        .when(schemaIntrospector.allowedTables())
        .thenReturn(
            Set.of(
                "accounts", "analytics.accounts",
                "categories", "analytics.categories",
                "transactions", "analytics.transactions",
                "budgets", "analytics.budgets",
                "monthly_summary", "analytics.monthly_summary"));

    SqlSafetyProperties properties =
        new SqlSafetyProperties(
            "jdbc:postgresql://localhost:5432/financecopilot",
            "finance_ro",
            "changeme",
            1000,
            Duration.ofSeconds(5),
            "analytics");

    validator = new SqlSafetyValidator(schemaIntrospector, properties);
  }

  // --- Structural / multi-statement ---

  @Test
  void rejectsBlankSql() {
    assertThatThrownBy(() -> validator.validate("  "))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsUnparsableSql() {
    assertThatThrownBy(() -> validator.validate("SELECT FROM WHERE"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsExplicitMultiStatementWithTrailingDdl() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    "SELECT * FROM analytics.accounts; DROP TABLE analytics.accounts;"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Exactly one SQL statement");
  }

  @Test
  void rejectsTwoSelectStatements() {
    assertThatThrownBy(() -> validator.validate("SELECT 1; SELECT 2;"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Exactly one SQL statement");
  }

  @Test
  void rejectsSecondStatementDisguisedNearAComment() {
    // The "--" comment only swallows the rest of *its own line*; the newline lets a real second
    // statement through. A naive semicolon-split validator would miss this; JSqlParser doesn't.
    String sql =
        "SELECT * FROM analytics.accounts -- looks harmless\n" + "; DROP TABLE analytics.accounts;";
    assertThatThrownBy(() -> validator.validate(sql))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void acceptsQueryWithMaliciousTextFullyInsideATrailingComment() {
    // Here the whole "; DROP TABLE ..." is on the same commented line, so it never becomes a
    // second statement — this must NOT be rejected as a false positive.
    String sql = "SELECT * FROM analytics.accounts -- ; DROP TABLE analytics.accounts;";
    String result = validator.validate(sql);
    assertThat(result).containsIgnoringCase("SELECT");
  }

  @Test
  void rejectsNonSelectStatementType() {
    assertThatThrownBy(() -> validator.validate("EXPLAIN SELECT * FROM analytics.accounts"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Only SELECT statements are allowed");
  }

  // --- Forbidden DDL/DML/etc tokens ---

  @Test
  void rejectsBareDropTable() {
    assertThatThrownBy(() -> validator.validate("DROP TABLE analytics.accounts"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsDelete() {
    assertThatThrownBy(() -> validator.validate("DELETE FROM analytics.transactions"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsUpdate() {
    assertThatThrownBy(() -> validator.validate("UPDATE analytics.accounts SET name = 'x'"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsInsert() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    "INSERT INTO analytics.accounts (name, account_type) VALUES ('x','CHECKING')"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsTruncate() {
    assertThatThrownBy(() -> validator.validate("TRUNCATE analytics.transactions"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsAlter() {
    assertThatThrownBy(
            () -> validator.validate("ALTER TABLE analytics.accounts ADD COLUMN hacked TEXT"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsCreate() {
    assertThatThrownBy(() -> validator.validate("CREATE TABLE evil (id int)"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsGrant() {
    assertThatThrownBy(() -> validator.validate("GRANT ALL ON analytics.accounts TO PUBLIC"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsRevoke() {
    assertThatThrownBy(() -> validator.validate("REVOKE ALL ON analytics.accounts FROM finance_ro"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsCopy() {
    assertThatThrownBy(() -> validator.validate("COPY analytics.accounts TO '/tmp/out.csv'"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsCall() {
    assertThatThrownBy(() -> validator.validate("CALL some_procedure()"))
        .isInstanceOf(SqlSafetyViolationException.class);
  }

  @Test
  void rejectsDangerousFunctionCall() {
    assertThatThrownBy(() -> validator.validate("SELECT pg_sleep(100)"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("PG_SLEEP");
  }

  // --- Table allow-list ---

  @Test
  void rejectsDisallowedSystemTable() {
    assertThatThrownBy(() -> validator.validate("SELECT * FROM pg_catalog.pg_shadow"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Table not permitted");
  }

  @Test
  void rejectsDisallowedTableReferencedOnlyInSubquery() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    "SELECT * FROM analytics.accounts WHERE name IN (SELECT usename FROM pg_user)"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Table not permitted");
  }

  @Test
  void rejectsUnionExfiltrationAttempt() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    "SELECT name FROM analytics.accounts UNION SELECT usename FROM pg_user"))
        .isInstanceOf(SqlSafetyViolationException.class)
        .hasMessageContaining("Table not permitted");
  }

  // --- LIMIT enforcement ---

  @Test
  void injectsLimitWhenAbsent() {
    String result = validator.validate("SELECT * FROM analytics.accounts");
    assertThat(result).containsIgnoringCase("LIMIT 1000");
  }

  @Test
  void clampsLimitWhenExceedsMax() {
    String result = validator.validate("SELECT * FROM analytics.transactions LIMIT 5000");
    assertThat(result).containsIgnoringCase("LIMIT 1000");
  }

  @Test
  void preservesLimitWhenWithinMax() {
    String result = validator.validate("SELECT * FROM analytics.transactions LIMIT 50");
    assertThat(result).containsIgnoringCase("LIMIT 50");
  }

  // --- Positive control: legitimate multi-table query is accepted ---

  @Test
  void acceptsLegitimateJoinAcrossAllowedTables() {
    String result =
        validator.validate(
            "SELECT a.name, t.amount FROM analytics.accounts a "
                + "JOIN analytics.transactions t ON t.account_id = a.id LIMIT 10");
    assertThat(result).containsIgnoringCase("LIMIT 10");
  }
}
