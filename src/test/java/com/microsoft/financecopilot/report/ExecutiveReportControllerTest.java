package com.microsoft.financecopilot.report;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.financecopilot.ai.dto.ReportSection;
import com.microsoft.financecopilot.report.dto.ExecutiveReportResponse;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code app.ai.enabled=true} overrides the {@code local} profile's default so the
 * {@code @ConditionalOnProperty}-gated {@link ExecutiveReportController} bean actually exists in
 * this slice.
 */
@WebMvcTest(ExecutiveReportController.class)
@TestPropertySource(properties = "app.ai.enabled=true")
class ExecutiveReportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExecutiveReportService executiveReportService;

  @Test
  void generateReturnsTheServiceResult() throws Exception {
    YearMonth month = YearMonth.of(2026, 6);
    when(executiveReportService.generate(month))
        .thenReturn(
            new ExecutiveReportResponse(
                month, "# Executive Summary", List.of(new ReportSection("Summary", "...")), false));

    mockMvc
        .perform(
            post("/api/v1/reports/executive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\": 2026, \"month\": 6}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cached").value(false))
        .andExpect(jsonPath("$.markdown").value("# Executive Summary"));
  }

  @Test
  void generateRejectsAnOutOfRangeMonthWith400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports/executive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\": 2026, \"month\": 13}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.month").exists());
  }
}
