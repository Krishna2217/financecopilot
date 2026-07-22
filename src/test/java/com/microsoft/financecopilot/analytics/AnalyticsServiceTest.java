package com.microsoft.financecopilot.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.analytics.dto.CashflowPoint;
import com.microsoft.financecopilot.analytics.dto.CategorySpend;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.analytics.dto.TrendPoint;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

  @Mock private AnalyticsRepository analyticsRepository;

  private AnalyticsService analyticsService;

  @Test
  void getKpisDelegatesToRepositoryForTheGivenMonth() {
    analyticsService = new AnalyticsService(analyticsRepository);
    YearMonth month = YearMonth.of(2026, 6);
    KpiSummary expected =
        new KpiSummary(
            month,
            BigDecimal.valueOf(5200),
            BigDecimal.valueOf(3120.45),
            BigDecimal.valueOf(2079.55),
            BigDecimal.valueOf(39.99));
    when(analyticsRepository.fetchKpis(month)).thenReturn(expected);

    KpiSummary result = analyticsService.getKpis(month);

    assertThat(result).isEqualTo(expected);
    verify(analyticsRepository).fetchKpis(month);
  }

  @Test
  void getSpendByCategoryDelegatesToRepositoryForTheGivenMonth() {
    analyticsService = new AnalyticsService(analyticsRepository);
    YearMonth month = YearMonth.of(2026, 6);
    List<CategorySpend> expected =
        List.of(new CategorySpend("Groceries", BigDecimal.valueOf(412.35), 18));
    when(analyticsRepository.fetchSpendByCategory(month)).thenReturn(expected);

    List<CategorySpend> result = analyticsService.getSpendByCategory(month);

    assertThat(result).isEqualTo(expected);
    verify(analyticsRepository).fetchSpendByCategory(month);
  }

  @Test
  void getCashflowDelegatesToRepositoryForTheGivenWindow() {
    analyticsService = new AnalyticsService(analyticsRepository);
    List<CashflowPoint> expected =
        List.of(
            new CashflowPoint(
                YearMonth.of(2026, 6),
                BigDecimal.valueOf(5200),
                BigDecimal.valueOf(3120.45),
                BigDecimal.valueOf(2079.55)));
    when(analyticsRepository.fetchCashflow(6)).thenReturn(expected);

    List<CashflowPoint> result = analyticsService.getCashflow(6);

    assertThat(result).isEqualTo(expected);
    verify(analyticsRepository).fetchCashflow(6);
  }

  @Test
  void getTrendDelegatesToRepositoryForTheGivenCategoryAndWindow() {
    analyticsService = new AnalyticsService(analyticsRepository);
    List<TrendPoint> expected =
        List.of(new TrendPoint(YearMonth.of(2026, 6), BigDecimal.valueOf(412.35)));
    when(analyticsRepository.fetchTrend("Groceries", 6)).thenReturn(expected);

    List<TrendPoint> result = analyticsService.getTrend("Groceries", 6);

    assertThat(result).isEqualTo(expected);
    verify(analyticsRepository).fetchTrend("Groceries", 6);
  }

  @Test
  void getTrendSupportsOmittingTheCategoryForAnOverallTrend() {
    analyticsService = new AnalyticsService(analyticsRepository);
    List<TrendPoint> expected =
        List.of(new TrendPoint(YearMonth.of(2026, 6), BigDecimal.valueOf(3120.45)));
    when(analyticsRepository.fetchTrend(null, 6)).thenReturn(expected);

    List<TrendPoint> result = analyticsService.getTrend(null, 6);

    assertThat(result).isEqualTo(expected);
    verify(analyticsRepository).fetchTrend(null, 6);
  }
}
