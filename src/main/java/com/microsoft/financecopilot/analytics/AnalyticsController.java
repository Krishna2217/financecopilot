package com.microsoft.financecopilot.analytics;

import com.microsoft.financecopilot.analytics.dto.CashflowPoint;
import com.microsoft.financecopilot.analytics.dto.CategorySpend;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.analytics.dto.TrendPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard analytics endpoints — parameterized repository queries only, no AI involved. */
@RestController
@RequestMapping("/api/v1/analytics")
@Validated
@Tag(name = "Analytics", description = "Dashboard analytics endpoints (no AI)")
public class AnalyticsController {

  private static final String MONTH_PATTERN = "\\d{4}-\\d{2}";

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/kpis")
  @Operation(summary = "Income, expenses, net cashflow, and savings rate for a month")
  public KpiSummary kpis(
      @RequestParam(required = false)
          @Pattern(regexp = MONTH_PATTERN, message = "month must be in yyyy-MM format")
          @Parameter(example = "2026-06", description = "Defaults to the current month")
          String month) {
    return analyticsService.getKpis(parseMonthOrCurrent(month));
  }

  @GetMapping("/spend-by-category")
  @Operation(summary = "Spend broken down by category for a month")
  public List<CategorySpend> spendByCategory(
      @RequestParam(required = false)
          @Pattern(regexp = MONTH_PATTERN, message = "month must be in yyyy-MM format")
          @Parameter(example = "2026-06", description = "Defaults to the current month")
          String month) {
    return analyticsService.getSpendByCategory(parseMonthOrCurrent(month));
  }

  @GetMapping("/cashflow")
  @Operation(summary = "Monthly income/expenses/net cashflow series over a trailing window")
  public List<CashflowPoint> cashflow(
      @RequestParam(defaultValue = "6")
          @Min(1)
          @Max(36)
          @Parameter(description = "Number of trailing months, 1-36")
          int months) {
    return analyticsService.getCashflow(months);
  }

  @GetMapping("/trend")
  @Operation(summary = "Monthly spend trend, overall or for a single category")
  public List<TrendPoint> trend(
      @RequestParam(required = false)
          @Parameter(example = "Groceries", description = "Omit for overall expense trend")
          String category,
      @RequestParam(defaultValue = "6")
          @Min(1)
          @Max(36)
          @Parameter(description = "Number of trailing months, 1-36")
          int months) {
    return analyticsService.getTrend(category, months);
  }

  private YearMonth parseMonthOrCurrent(String month) {
    return month != null ? YearMonth.parse(month) : YearMonth.now();
  }
}
