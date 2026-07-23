package com.microsoft.financecopilot.anomaly;

import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anomalies")
@Tag(name = "Anomalies", description = "Spending anomaly detection and AI explanation")
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AnomalyExplanationController {

  private final AnomalyExplanationService anomalyExplanationService;

  public AnomalyExplanationController(AnomalyExplanationService anomalyExplanationService) {
    this.anomalyExplanationService = anomalyExplanationService;
  }

  @PostMapping("/{id}/explain")
  @Operation(summary = "Generate an AI explanation for a previously detected anomaly")
  public AnomalyResponse explain(@PathVariable Long id) {
    return anomalyExplanationService.explain(id);
  }
}
