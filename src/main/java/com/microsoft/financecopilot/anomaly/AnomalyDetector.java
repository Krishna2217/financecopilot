package com.microsoft.financecopilot.anomaly;

import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import java.sql.Date;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * Z-score and IQR anomaly detection over monthly spend per category — computed entirely in SQL
 * (window/aggregate functions), no AI involved. A category's current-month spend is compared
 * against its own historical baseline (all prior months, minimum 3 data points).
 */
@Component
public class AnomalyDetector {

  private static final String STATS_CTE =
      "WITH category_monthly AS ("
          + "  SELECT c.id AS category_id, c.name AS category_name, ms.summary_month, SUM(-ms.total_amount) AS spend"
          + "  FROM analytics.monthly_summary ms"
          + "  JOIN analytics.categories c ON c.id = ms.category_id"
          + "  WHERE c.is_income = false"
          + "  GROUP BY c.id, c.name, ms.summary_month"
          + "), current_month AS ("
          + "  SELECT category_id, category_name, spend FROM category_monthly WHERE summary_month = ?"
          + "), baseline AS ("
          + "  SELECT category_id,"
          + "         AVG(spend) AS mean_spend,"
          + "         STDDEV_SAMP(spend) AS stddev_spend,"
          + "         PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY spend) AS q1,"
          + "         PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY spend) AS q3,"
          + "         COUNT(*) AS sample_size"
          + "  FROM category_monthly"
          + "  WHERE summary_month < ?"
          + "  GROUP BY category_id"
          + "), stats AS ("
          + "  SELECT cm.category_id, cm.category_name, cm.spend,"
          + "         b.mean_spend,"
          + "         CASE WHEN b.stddev_spend IS NULL OR b.stddev_spend = 0 THEN 0"
          + "              ELSE (cm.spend - b.mean_spend) / b.stddev_spend END AS z_score,"
          + "         CASE WHEN b.q1 IS NULL OR b.q3 IS NULL THEN false"
          + "              WHEN cm.spend < b.q1 - 1.5 * (b.q3 - b.q1) THEN true"
          + "              WHEN cm.spend > b.q3 + 1.5 * (b.q3 - b.q1) THEN true"
          + "              ELSE false END AS iqr_flag"
          + "  FROM current_month cm"
          + "  JOIN baseline b ON b.category_id = cm.category_id"
          + "  WHERE b.sample_size >= 3"
          + ")";

  private final JdbcTemplate jdbcTemplate;

  public AnomalyDetector(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * All categories whose current-month spend is a statistical anomaly relative to their own
   * history.
   */
  public List<DetectedAnomaly> detect(YearMonth month) {
    String sql =
        STATS_CTE
            + " SELECT * FROM stats WHERE ABS(z_score) > 2 OR iqr_flag = true ORDER BY ABS(z_score) DESC";
    Date monthDate = Date.valueOf(month.atDay(1));
    return jdbcTemplate.query(sql, rowMapper(month), monthDate, monthDate);
  }

  /** Stats for one category/month regardless of whether it's currently flagged as an anomaly. */
  public Optional<DetectedAnomaly> detectForCategory(Long categoryId, YearMonth month) {
    String sql = STATS_CTE + " SELECT * FROM stats WHERE category_id = ?";
    Date monthDate = Date.valueOf(month.atDay(1));
    List<DetectedAnomaly> results =
        jdbcTemplate.query(sql, rowMapper(month), monthDate, monthDate, categoryId);
    return results.stream().findFirst();
  }

  private RowMapper<DetectedAnomaly> rowMapper(YearMonth month) {
    return (rs, rowNum) ->
        new DetectedAnomaly(
            rs.getLong("category_id"),
            rs.getString("category_name"),
            month,
            rs.getBigDecimal("spend"),
            rs.getBigDecimal("mean_spend"),
            rs.getBigDecimal("z_score"),
            rs.getBoolean("iqr_flag"));
  }
}
