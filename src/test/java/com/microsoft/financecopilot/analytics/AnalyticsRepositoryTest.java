package com.microsoft.financecopilot.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class AnalyticsRepositoryTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private ResultSet resultSet;

  private AnalyticsRepository repository;

  @Test
  void fetchKpisComputesSavingsRateFromIncomeAndNetCashflow() throws Exception {
    repository = new AnalyticsRepository(jdbcTemplate);
    YearMonth month = YearMonth.of(2026, 6);

    when(resultSet.getBigDecimal("total_income")).thenReturn(BigDecimal.valueOf(1000));
    when(resultSet.getBigDecimal("total_expenses")).thenReturn(BigDecimal.valueOf(600));
    when(resultSet.getBigDecimal("net_cashflow")).thenReturn(BigDecimal.valueOf(400));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<RowMapper<KpiSummary>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
    when(jdbcTemplate.queryForObject(
            anyString(), mapperCaptor.capture(), eq(Date.valueOf(month.atDay(1)))))
        .thenAnswer(invocation -> mapperCaptor.getValue().mapRow(resultSet, 0));

    KpiSummary result = repository.fetchKpis(month);

    assertThat(result.totalIncome()).isEqualByComparingTo("1000");
    assertThat(result.netCashflow()).isEqualByComparingTo("400");
    assertThat(result.savingsRatePercent()).isEqualByComparingTo("40.0000");
  }

  @Test
  void fetchKpisGuardsAgainstDivideByZeroWhenThereIsNoIncome() throws Exception {
    repository = new AnalyticsRepository(jdbcTemplate);
    YearMonth month = YearMonth.of(2026, 6);

    when(resultSet.getBigDecimal("total_income")).thenReturn(BigDecimal.ZERO);
    when(resultSet.getBigDecimal("total_expenses")).thenReturn(BigDecimal.valueOf(200));
    when(resultSet.getBigDecimal("net_cashflow")).thenReturn(BigDecimal.valueOf(-200));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<RowMapper<KpiSummary>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
    when(jdbcTemplate.queryForObject(anyString(), mapperCaptor.capture(), any(Date.class)))
        .thenAnswer(invocation -> mapperCaptor.getValue().mapRow(resultSet, 0));

    KpiSummary result = repository.fetchKpis(month);

    assertThat(result.savingsRatePercent()).isEqualByComparingTo("0");
  }

  @Test
  void fetchTrendUsesTheCategoryFilteredQueryWhenCategoryIsProvided() {
    repository = new AnalyticsRepository(jdbcTemplate);

    repository.fetchTrend("Groceries", 6);

    verify(jdbcTemplate)
        .query(
            anyString(),
            org.mockito.ArgumentMatchers.<RowMapper<?>>any(),
            eq("Groceries"),
            any(Date.class));
  }

  @Test
  void fetchTrendUsesTheOverallExpenseQueryWhenCategoryIsOmitted() {
    repository = new AnalyticsRepository(jdbcTemplate);

    repository.fetchTrend(null, 6);

    verify(jdbcTemplate)
        .query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<?>>any(), any(Date.class));
  }
}
