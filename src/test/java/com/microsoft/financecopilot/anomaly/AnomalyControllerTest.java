package com.microsoft.financecopilot.anomaly;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnomalyController.class)
class AnomalyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnomalyDetectionService anomalyDetectionService;

  @Test
  void anomaliesDefaultsToTheCurrentMonth() throws Exception {
    YearMonth currentMonth = YearMonth.now();
    when(anomalyDetectionService.getAnomalies(currentMonth))
        .thenReturn(
            List.of(
                new AnomalyResponse(
                    1L,
                    2L,
                    "Utilities",
                    currentMonth,
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(12.65),
                    true,
                    null)));

    mockMvc
        .perform(get("/api/v1/anomalies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].categoryName").value("Utilities"))
        .andExpect(jsonPath("$[0].iqrFlag").value(true));
  }

  @Test
  void anomaliesRejectsAMalformedMonthWith400() throws Exception {
    mockMvc
        .perform(get("/api/v1/anomalies").param("month", "bad-format"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.month").exists());
  }
}
