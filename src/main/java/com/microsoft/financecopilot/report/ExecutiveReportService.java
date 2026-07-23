package com.microsoft.financecopilot.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.financecopilot.ai.dto.ExecutiveReport;
import com.microsoft.financecopilot.ai.dto.ReportSection;
import com.microsoft.financecopilot.analytics.AnalyticsService;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.anomaly.AnomalyDetectionService;
import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import com.microsoft.financecopilot.report.dto.ExecutiveReportResponse;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Assembles KPIs + top anomalies for a month, asks {@code reportChatClient} (grounded in the
 * glossary via a filtered {@code QuestionAnswerAdvisor}) for an executive summary, and persists it.
 * Idempotent by {@code {year, month}}: a second call for the same month returns the cached row
 * instead of calling the model again.
 *
 * <p>Gated by {@code app.ai.enabled} like the rest of the AI wiring.
 */
@Service
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ExecutiveReportService {

  private static final int TOP_ANOMALIES_LIMIT = 5;

  private final ChatClient reportChatClient;
  private final AnalyticsService analyticsService;
  private final AnomalyDetectionService anomalyDetectionService;
  private final ExecutiveReportRepository executiveReportRepository;
  private final ObjectMapper objectMapper;

  public ExecutiveReportService(
      @Qualifier("reportChatClient") ChatClient reportChatClient,
      AnalyticsService analyticsService,
      AnomalyDetectionService anomalyDetectionService,
      ExecutiveReportRepository executiveReportRepository,
      ObjectMapper objectMapper) {
    this.reportChatClient = reportChatClient;
    this.analyticsService = analyticsService;
    this.anomalyDetectionService = anomalyDetectionService;
    this.executiveReportRepository = executiveReportRepository;
    this.objectMapper = objectMapper;
  }

  public ExecutiveReportResponse generate(YearMonth month) {
    var existing =
        executiveReportRepository.findByReportYearAndReportMonth(
            month.getYear(), month.getMonthValue());
    if (existing.isPresent()) {
      return toResponse(existing.get(), month, true);
    }

    KpiSummary kpis = analyticsService.getKpis(month);
    List<AnomalyResponse> topAnomalies =
        anomalyDetectionService.getAnomalies(month).stream().limit(TOP_ANOMALIES_LIMIT).toList();

    ExecutiveReport report =
        reportChatClient
            .prompt()
            .user(buildPrompt(month, kpis, topAnomalies))
            .call()
            .entity(ExecutiveReport.class);

    ExecutiveReportEntity entity = new ExecutiveReportEntity();
    entity.setReportYear(month.getYear());
    entity.setReportMonth(month.getMonthValue());
    entity.setMarkdown(report.markdown());
    entity.setSections(writeSectionsAsJson(report.sections()));
    entity.setCreatedAt(Instant.now());
    ExecutiveReportEntity saved = executiveReportRepository.save(entity);

    return new ExecutiveReportResponse(month, saved.getMarkdown(), report.sections(), false);
  }

  private String buildPrompt(YearMonth month, KpiSummary kpis, List<AnomalyResponse> anomalies) {
    StringBuilder sb = new StringBuilder();
    sb.append("Month: ").append(month).append('\n');
    sb.append("Total income: ").append(kpis.totalIncome()).append('\n');
    sb.append("Total expenses: ").append(kpis.totalExpenses()).append('\n');
    sb.append("Net cashflow: ").append(kpis.netCashflow()).append('\n');
    sb.append("Savings rate: ").append(kpis.savingsRatePercent()).append("%\n");
    sb.append("Top anomalies:\n");
    if (anomalies.isEmpty()) {
      sb.append("  None detected.\n");
    } else {
      for (AnomalyResponse anomaly : anomalies) {
        sb.append("  - ")
            .append(anomaly.categoryName())
            .append(": spend ")
            .append(anomaly.spend())
            .append(" vs historical average ")
            .append(anomaly.meanSpend())
            .append(" (z-score ")
            .append(anomaly.zScore())
            .append(")\n");
      }
    }
    return sb.toString();
  }

  private String writeSectionsAsJson(List<ReportSection> sections) {
    try {
      return objectMapper.writeValueAsString(sections);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to serialize report sections", e);
    }
  }

  private List<ReportSection> readSectionsFromJson(String json) {
    try {
      return objectMapper.readerForListOf(ReportSection.class).readValue(json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to deserialize report sections", e);
    }
  }

  private ExecutiveReportResponse toResponse(
      ExecutiveReportEntity entity, YearMonth month, boolean cached) {
    return new ExecutiveReportResponse(
        month, entity.getMarkdown(), readSectionsFromJson(entity.getSections()), cached);
  }
}
