package com.microsoft.financecopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.financecopilot.ai.dto.ExecutiveReport;
import com.microsoft.financecopilot.ai.dto.ReportSection;
import com.microsoft.financecopilot.analytics.AnalyticsService;
import com.microsoft.financecopilot.analytics.dto.KpiSummary;
import com.microsoft.financecopilot.anomaly.AnomalyDetectionService;
import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import com.microsoft.financecopilot.report.dto.ExecutiveReportResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExecutiveReportServiceTest {

  @Mock private AnalyticsService analyticsService;
  @Mock private AnomalyDetectionService anomalyDetectionService;
  @Mock private ExecutiveReportRepository executiveReportRepository;

  private ChatClient reportChatClient;
  private ChatClient.ChatClientRequestSpec requestSpec;
  private ChatClient.CallResponseSpec callSpec;

  private ExecutiveReportService service;

  private static final YearMonth MONTH = YearMonth.of(2026, 6);

  @BeforeEach
  void setUp() {
    reportChatClient = mock(ChatClient.class);
    requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    callSpec = mock(ChatClient.CallResponseSpec.class);

    service =
        new ExecutiveReportService(
            reportChatClient,
            analyticsService,
            anomalyDetectionService,
            executiveReportRepository,
            new ObjectMapper());
  }

  @Test
  void generatesAndPersistsAStableReportWhenNoneExistsYet() {
    when(executiveReportRepository.findByReportYearAndReportMonth(2026, 6))
        .thenReturn(Optional.empty());
    when(analyticsService.getKpis(MONTH))
        .thenReturn(
            new KpiSummary(
                MONTH,
                BigDecimal.valueOf(5200),
                BigDecimal.valueOf(3120),
                BigDecimal.valueOf(2080),
                BigDecimal.valueOf(40)));
    when(anomalyDetectionService.getAnomalies(MONTH))
        .thenReturn(
            List.of(
                new AnomalyResponse(
                    1L,
                    2L,
                    "Utilities",
                    MONTH,
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(12.65),
                    true,
                    null)));

    ExecutiveReport aiResult =
        new ExecutiveReport(
            "# Executive Summary\n\nStable content.",
            List.of(new ReportSection("Summary", "Stable content.")));
    when(reportChatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callSpec);
    when(callSpec.entity(ExecutiveReport.class)).thenReturn(aiResult);

    when(executiveReportRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              ExecutiveReportEntity entity = invocation.getArgument(0);
              ReflectionTestUtils.setField(entity, "id", 1L);
              return entity;
            });

    ExecutiveReportResponse response = service.generate(MONTH);

    assertThat(response.cached()).isFalse();
    assertThat(response.markdown()).isEqualTo("# Executive Summary\n\nStable content.");
    assertThat(response.sections()).hasSize(1);
    assertThat(response.sections().get(0).title()).isEqualTo("Summary");
  }

  @Test
  void regenerationForAnAlreadyReportedMonthReturnsTheCachedRowWithoutCallingTheModel()
      throws Exception {
    ExecutiveReportEntity existing = new ExecutiveReportEntity();
    existing.setReportYear(2026);
    existing.setReportMonth(6);
    existing.setMarkdown("# Cached report");
    existing.setSections(
        new ObjectMapper().writeValueAsString(List.of(new ReportSection("Summary", "Cached."))));
    existing.setCreatedAt(Instant.now());
    ReflectionTestUtils.setField(existing, "id", 9L);

    when(executiveReportRepository.findByReportYearAndReportMonth(2026, 6))
        .thenReturn(Optional.of(existing));

    ExecutiveReportResponse response = service.generate(MONTH);

    assertThat(response.cached()).isTrue();
    assertThat(response.markdown()).isEqualTo("# Cached report");
    assertThat(response.sections()).hasSize(1);
    assertThat(response.sections().get(0).content()).isEqualTo("Cached.");

    verify(reportChatClient, never()).prompt();
    verify(executiveReportRepository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(analyticsService, never()).getKpis(org.mockito.ArgumentMatchers.any());
  }
}
