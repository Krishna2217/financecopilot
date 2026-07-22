package com.microsoft.financecopilot.analytics;

import com.microsoft.financecopilot.analytics.dto.CashflowPoint;
import com.microsoft.financecopilot.analytics.dto.CategorySpend;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.analytics.dto.TrendPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.YearMonth;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Parameterized, read-only aggregation queries over the pre-aggregated {@code
 * analytics.monthly_summary} table — no AI involved anywhere in this package.
 */
@Repository
public class AnalyticsRepository {

  private final JdbcTemplate jdbcTemplate;

  public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public KpiSummary fetchKpis(YearMonth month) {
    String sql =
        "SELECT "
            + "COALESCE(SUM(CASE WHEN c.is_income THEN ms.total_amount ELSE 0 END), 0) AS total_income, "
            + "COALESCE(SUM(CASE WHEN NOT c.is_income THEN -ms.total_amount ELSE 0 END), 0) AS total_expenses, "
            + "COALESCE(SUM(ms.total_amount), 0) AS net_cashflow "
            + "FROM analytics.monthly_summary ms "
            + "JOIN analytics.categories c ON c.id = ms.category_id "
            + "WHERE ms.summary_month = ?";
    return jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) -> {
          BigDecimal income = rs.getBigDecimal("total_income");
          BigDecimal expenses = rs.getBigDecimal("total_expenses");
          BigDecimal net = rs.getBigDecimal("net_cashflow");
          BigDecimal savingsRate =
              income.compareTo(BigDecimal.ZERO) == 0
                  ? BigDecimal.ZERO
                  : net.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
          return new KpiSummary(month, income, expenses, net, savingsRate);
        },
        Date.valueOf(month.atDay(1)));
  }

  public List<CategorySpend> fetchSpendByCategory(YearMonth month) {
    String sql =
        "SELECT c.name AS category, SUM(-ms.total_amount) AS total_amount, "
            + "SUM(ms.transaction_count) AS transaction_count "
            + "FROM analytics.monthly_summary ms "
            + "JOIN analytics.categories c ON c.id = ms.category_id "
            + "WHERE ms.summary_month = ? AND c.is_income = false "
            + "GROUP BY c.name ORDER BY total_amount DESC";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new CategorySpend(
                rs.getString("category"),
                rs.getBigDecimal("total_amount"),
                rs.getLong("transaction_count")),
        Date.valueOf(month.atDay(1)));
  }

  public List<CashflowPoint> fetchCashflow(int months) {
    String sql =
        "SELECT ms.summary_month, "
            + "SUM(CASE WHEN c.is_income THEN ms.total_amount ELSE 0 END) AS income, "
            + "SUM(CASE WHEN NOT c.is_income THEN -ms.total_amount ELSE 0 END) AS expenses, "
            + "SUM(ms.total_amount) AS net_cashflow "
            + "FROM analytics.monthly_summary ms "
            + "JOIN analytics.categories c ON c.id = ms.category_id "
            + "WHERE ms.summary_month >= ? "
            + "GROUP BY ms.summary_month ORDER BY ms.summary_month";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new CashflowPoint(
                YearMonth.from(rs.getDate("summary_month").toLocalDate()),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("expenses"),
                rs.getBigDecimal("net_cashflow")),
        Date.valueOf(startOfWindow(months)));
  }

  public List<TrendPoint> fetchTrend(String category, int months) {
    var mapper =
        (org.springframework.jdbc.core.RowMapper<TrendPoint>)
            (rs, rowNum) ->
                new TrendPoint(
                    YearMonth.from(rs.getDate("summary_month").toLocalDate()),
                    rs.getBigDecimal("total_amount"));

    if (category != null) {
      String sql =
          "SELECT ms.summary_month, SUM(-ms.total_amount) AS total_amount "
              + "FROM analytics.monthly_summary ms "
              + "JOIN analytics.categories c ON c.id = ms.category_id "
              + "WHERE c.name = ? AND ms.summary_month >= ? "
              + "GROUP BY ms.summary_month ORDER BY ms.summary_month";
      return jdbcTemplate.query(sql, mapper, category, Date.valueOf(startOfWindow(months)));
    }
    String sql =
        "SELECT ms.summary_month, SUM(-ms.total_amount) AS total_amount "
            + "FROM analytics.monthly_summary ms "
            + "JOIN analytics.categories c ON c.id = ms.category_id "
            + "WHERE c.is_income = false AND ms.summary_month >= ? "
            + "GROUP BY ms.summary_month ORDER BY ms.summary_month";
    return jdbcTemplate.query(sql, mapper, Date.valueOf(startOfWindow(months)));
  }

  private java.time.LocalDate startOfWindow(int months) {
    return YearMonth.now().minusMonths(months - 1L).atDay(1);
  }
}
