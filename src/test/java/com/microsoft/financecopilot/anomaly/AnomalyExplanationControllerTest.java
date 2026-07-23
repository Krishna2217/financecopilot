package com.microsoft.financecopilot.anomaly;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code app.ai.enabled=true} overrides the {@code local} profile's default so the
 * {@code @ConditionalOnProperty}-gated {@link AnomalyExplanationController} bean actually exists in
 * this slice.
 */
@WebMvcTest(AnomalyExplanationController.class)
@TestPropertySource(properties = "app.ai.enabled=true")
class AnomalyExplanationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnomalyExplanationService anomalyExplanationService;

  @Test
  void explainReturnsTheServiceResult() throws Exception {
    YearMonth month = YearMonth.of(2026, 6);
    when(anomalyExplanationService.explain(7L))
        .thenReturn(
            new AnomalyResponse(
                7L,
                2L,
                "Utilities",
                month,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(12.65),
                true,
                "Your utilities spend doubled compared to your usual average."));

    mockMvc
        .perform(post("/api/v1/anomalies/7/explain"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.explanation")
                .value("Your utilities spend doubled compared to your usual average."));
  }
}
