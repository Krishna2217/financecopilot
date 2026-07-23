package com.microsoft.financecopilot.anomaly;

import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Statistical anomaly detection — z-score + IQR over monthly spend per category, no AI. */
@RestController
@RequestMapping("/api/v1/anomalies")
@Validated
@Tag(name = "Anomalies", description = "Spending anomaly detection and AI explanation")
public class AnomalyController {

  private static final String MONTH_PATTERN = "\\d{4}-\\d{2}";

  private final AnomalyDetectionService anomalyDetectionService;

  public AnomalyController(AnomalyDetectionService anomalyDetectionService) {
    this.anomalyDetectionService = anomalyDetectionService;
  }

  @GetMapping
  @Operation(summary = "Detect spending anomalies (z-score + IQR) for a month")
  public List<AnomalyResponse> anomalies(
      @RequestParam(required = false)
          @Pattern(regexp = MONTH_PATTERN, message = "month must be in yyyy-MM format")
          @Parameter(example = "2026-06", description = "Defaults to the current month")
          String month) {
    YearMonth yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
    return anomalyDetectionService.getAnomalies(yearMonth);
  }
}
