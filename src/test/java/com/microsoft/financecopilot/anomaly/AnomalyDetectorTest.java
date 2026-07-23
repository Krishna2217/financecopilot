package com.microsoft.financecopilot.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises the detector's z-score/IQR SQL against real fixture data in a real Postgres, with
 * expected values worked out by hand:
 *
 * <ul>
 *   <li>Utilities: 5 baseline months (100, 110, 90, 105, 95) — mean 100, stddev_samp ≈7.906,
 *       Q1=95/Q3=105 (IQR bounds [80, 120]). Current month spend 200 → z ≈12.65, outside IQR bounds
 *       → flagged.
 *   <li>Entertainment: 5 baseline months (50, 55, 45, 52, 48) — mean 50, stddev_samp ≈3.808,
 *       Q1=48/Q3=52 (IQR bounds [42, 58]). Current month spend 53 → z ≈0.79, inside IQR bounds →
 *       not flagged.
 * </ul>
 */
@Testcontainers
class AnomalyDetectorTest {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse("postgres:17").asCompatibleSubstituteFor("postgres"));

  private static HikariDataSource dataSource;
  private static JdbcTemplate jdbcTemplate;
  private static AnomalyDetector detector;

  private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 6);

  @BeforeAll
  static void setUp() {
    POSTGRES.start();

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(POSTGRES.getJdbcUrl());
    config.setUsername(POSTGRES.getUsername());
    config.setPassword(POSTGRES.getPassword());
    dataSource = new HikariDataSource(config);
    jdbcTemplate = new JdbcTemplate(dataSource);

    jdbcTemplate.execute("CREATE SCHEMA analytics");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.categories (id BIGSERIAL PRIMARY KEY, name VARCHAR(80) NOT NULL, "
            + "is_income BOOLEAN NOT NULL DEFAULT false)");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.monthly_summary (id BIGSERIAL PRIMARY KEY, account_id BIGINT NOT NULL, "
            + "category_id BIGINT NOT NULL, summary_month DATE NOT NULL, total_amount NUMERIC(14,2) NOT NULL, "
            + "transaction_count INT NOT NULL)");

    jdbcTemplate.update(
        "INSERT INTO analytics.categories (id, name, is_income) VALUES (1, 'Utilities', false)");
    jdbcTemplate.update(
        "INSERT INTO analytics.categories (id, name, is_income) VALUES (2, 'Entertainment', false)");

    insertMonth(1, "2026-01-01", -100);
    insertMonth(1, "2026-02-01", -110);
    insertMonth(1, "2026-03-01", -90);
    insertMonth(1, "2026-04-01", -105);
    insertMonth(1, "2026-05-01", -95);
    insertMonth(1, "2026-06-01", -200); // anomalous spike

    insertMonth(2, "2026-01-01", -50);
    insertMonth(2, "2026-02-01", -55);
    insertMonth(2, "2026-03-01", -45);
    insertMonth(2, "2026-04-01", -52);
    insertMonth(2, "2026-05-01", -48);
    insertMonth(2, "2026-06-01", -53); // normal

    detector = new AnomalyDetector(jdbcTemplate);
  }

  private static void insertMonth(long categoryId, String month, double totalAmount) {
    jdbcTemplate.update(
        "INSERT INTO analytics.monthly_summary (account_id, category_id, summary_month, total_amount, transaction_count) "
            + "VALUES (1, ?, ?::date, ?, 10)",
        categoryId,
        month,
        totalAmount);
  }

  @AfterAll
  static void tearDown() {
    dataSource.close();
    POSTGRES.stop();
  }

  @Test
  void flagsTheStatisticallyAnomalousCategoryButNotTheNormalOne() {
    List<DetectedAnomaly> anomalies = detector.detect(CURRENT_MONTH);

    assertThat(anomalies).extracting(DetectedAnomaly::categoryName).containsExactly("Utilities");

    DetectedAnomaly utilities = anomalies.get(0);
    assertThat(utilities.spend()).isEqualByComparingTo("200");
    assertThat(utilities.meanSpend()).isEqualByComparingTo("100");
    assertThat(utilities.zScore().doubleValue())
        .isCloseTo(12.649, org.assertj.core.data.Offset.offset(0.01));
    assertThat(utilities.iqrFlag()).isTrue();
  }

  @Test
  void detectForCategoryReturnsStatsRegardlessOfWhetherFlagged() {
    Optional<DetectedAnomaly> entertainment = detector.detectForCategory(2L, CURRENT_MONTH);

    assertThat(entertainment).isPresent();
    assertThat(entertainment.get().categoryName()).isEqualTo("Entertainment");
    assertThat(entertainment.get().iqrFlag()).isFalse();
    assertThat(entertainment.get().zScore().doubleValue())
        .isCloseTo(0.788, org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  void detectForCategoryIsEmptyWhenNoBaselineHistoryExists() {
    Optional<DetectedAnomaly> missing = detector.detectForCategory(999L, CURRENT_MONTH);

    assertThat(missing).isEmpty();
  }
}
