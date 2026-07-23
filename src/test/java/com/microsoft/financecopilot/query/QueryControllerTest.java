package com.microsoft.financecopilot.query;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.financecopilot.query.dto.QueryResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code app.ai.enabled=true} overrides the {@code local} profile's default so the
 * {@code @ConditionalOnProperty}-gated {@link QueryController} bean actually exists in this slice.
 */
@WebMvcTest(QueryController.class)
@TestPropertySource(properties = "app.ai.enabled=true")
class QueryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private QueryService queryService;

  @Test
  void queryReturnsTheServiceResult() throws Exception {
    when(queryService.executeQuery("How much did I spend on groceries?"))
        .thenReturn(
            new QueryResponse(
                "SELECT sum(amount) FROM analytics.transactions LIMIT 1000",
                "Sums transaction amounts",
                List.of("sum"),
                List.of(Map.of("sum", -312.47)),
                "You spent $312.47.",
                42L));

    mockMvc
        .perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"naturalLanguageQuery\": \"How much did I spend on groceries?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historyId").value(42))
        .andExpect(jsonPath("$.summary").value("You spent $312.47."));
  }

  @Test
  void queryRejectsABlankQuestionWith400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"naturalLanguageQuery\": \"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.naturalLanguageQuery").exists());
  }
}
