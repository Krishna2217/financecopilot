package com.microsoft.financecopilot.report;

import com.microsoft.financecopilot.report.dto.ExecutiveReportRequest;
import com.microsoft.financecopilot.report.dto.ExecutiveReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/executive")
@Tag(name = "Reports", description = "AI-generated executive summaries")
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ExecutiveReportController {

  private final ExecutiveReportService executiveReportService;

  public ExecutiveReportController(ExecutiveReportService executiveReportService) {
    this.executiveReportService = executiveReportService;
  }

  @PostMapping
  @Operation(
      summary = "Generate (or return the cached) executive report for a month",
      description =
          "Idempotent by {year, month}: a second call for the same month returns the cached report.")
  public ExecutiveReportResponse generate(@Valid @RequestBody ExecutiveReportRequest request) {
    return executiveReportService.generate(YearMonth.of(request.year(), request.month()));
  }
}
