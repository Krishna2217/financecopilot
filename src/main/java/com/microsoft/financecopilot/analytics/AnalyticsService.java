package com.microsoft.financecopilot.analytics;

import com.microsoft.financecopilot.analytics.dto.CashflowPoint;
import com.microsoft.financecopilot.analytics.dto.CategorySpend;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.analytics.dto.TrendPoint;
import com.microsoft.financecopilot.config.CacheConfig;
import java.time.YearMonth;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

  private final AnalyticsRepository analyticsRepository;

  public AnalyticsService(AnalyticsRepository analyticsRepository) {
    this.analyticsRepository = analyticsRepository;
  }

  @Cacheable(cacheNames = CacheConfig.KPIS_CACHE, key = "#month")
  public KpiSummary getKpis(YearMonth month) {
    return analyticsRepository.fetchKpis(month);
  }

  @Cacheable(cacheNames = CacheConfig.SPEND_BY_CATEGORY_CACHE, key = "#month")
  public List<CategorySpend> getSpendByCategory(YearMonth month) {
    return analyticsRepository.fetchSpendByCategory(month);
  }

  @Cacheable(cacheNames = CacheConfig.CASHFLOW_CACHE, key = "#months")
  public List<CashflowPoint> getCashflow(int months) {
    return analyticsRepository.fetchCashflow(months);
  }

  @Cacheable(cacheNames = CacheConfig.TREND_CACHE, key = "(#category ?: '') + ':' + #months")
  public List<TrendPoint> getTrend(String category, int months) {
    return analyticsRepository.fetchTrend(category, months);
  }
}
