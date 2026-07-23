package com.microsoft.financecopilot.analytics;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.financecopilot.analytics.dto.CashflowPoint;
import com.microsoft.financecopilot.analytics.dto.CategorySpend;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.analytics.dto.TrendPoint;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnalyticsService analyticsService;

  @Test
  void kpisReturnsCurrentMonthByDefault() throws Exception {
    YearMonth currentMonth = YearMonth.now();
    when(analyticsService.getKpis(currentMonth))
        .thenReturn(
            new KpiSummary(
                currentMonth,
                BigDecimal.valueOf(5200),
                BigDecimal.valueOf(3120),
                BigDecimal.valueOf(2080),
                BigDecimal.valueOf(40)));

    mockMvc
        .perform(get("/api/v1/analytics/kpis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalIncome").value(5200))
        .andExpect(jsonPath("$.savingsRatePercent").value(40));
  }

  @Test
  void kpisRejectsAMalformedMonthWith400() throws Exception {
    mockMvc
        .perform(get("/api/v1/analytics/kpis").param("month", "not-a-month"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.month").exists());
  }

  @Test
  void spendByCategoryReturnsTheServiceResult() throws Exception {
    YearMonth month = YearMonth.of(2026, 6);
    when(analyticsService.getSpendByCategory(month))
        .thenReturn(List.of(new CategorySpend("Groceries", BigDecimal.valueOf(412.35), 18)));

    mockMvc
        .perform(get("/api/v1/analytics/spend-by-category").param("month", "2026-06"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].category").value("Groceries"))
        .andExpect(jsonPath("$[0].transactionCount").value(18));
  }

  @Test
  void cashflowRejectsMonthsOutOfRangeWith400() throws Exception {
    mockMvc
        .perform(get("/api/v1/analytics/cashflow").param("months", "99"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cashflowDefaultsToSixMonths() throws Exception {
    when(analyticsService.getCashflow(6))
        .thenReturn(
            List.of(
                new CashflowPoint(
                    YearMonth.of(2026, 6),
                    BigDecimal.valueOf(5200),
                    BigDecimal.valueOf(3120),
                    BigDecimal.valueOf(2080))));

    mockMvc.perform(get("/api/v1/analytics/cashflow")).andExpect(status().isOk());
  }

  @Test
  void trendSupportsAnOptionalCategoryFilter() throws Exception {
    when(analyticsService.getTrend(eq("Groceries"), anyInt()))
        .thenReturn(List.of(new TrendPoint(YearMonth.of(2026, 6), BigDecimal.valueOf(412.35))));

    mockMvc
        .perform(get("/api/v1/analytics/trend").param("category", "Groceries").param("months", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalAmount").value(412.35));
  }
}
