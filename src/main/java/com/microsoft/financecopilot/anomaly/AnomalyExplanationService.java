package com.microsoft.financecopilot.anomaly;

import com.microsoft.financecopilot.ai.dto.AnomalyExplanation;
import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import com.microsoft.financecopilot.common.exception.ResourceNotFoundException;
import java.time.YearMonth;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Explains a previously detected anomaly via {@code anomalyChatClient}.
 *
 * <p>Gated by {@code app.ai.enabled}, like the rest of the AI wiring: this depends directly on the
 * {@code anomalyChatClient} bean, which doesn't exist locally without Azure/OpenAI credentials.
 * Detection itself ({@link AnomalyDetectionService}) has no such dependency.
 */
@Service
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AnomalyExplanationService {

  private final ChatClient anomalyChatClient;
  private final AnomalyDetector anomalyDetector;
  private final AnomalyExplanationRepository anomalyExplanationRepository;

  public AnomalyExplanationService(
      @Qualifier("anomalyChatClient") ChatClient anomalyChatClient,
      AnomalyDetector anomalyDetector,
      AnomalyExplanationRepository anomalyExplanationRepository) {
    this.anomalyChatClient = anomalyChatClient;
    this.anomalyDetector = anomalyDetector;
    this.anomalyExplanationRepository = anomalyExplanationRepository;
  }

  public AnomalyResponse explain(Long id) {
    AnomalyExplanationEntity entity =
        anomalyExplanationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No anomaly found with id " + id));

    YearMonth month = YearMonth.from(entity.getSummaryMonth());
    DetectedAnomaly detected =
        anomalyDetector
            .detectForCategory(entity.getCategoryId(), month)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No spend history found for category "
                            + entity.getCategoryId()
                            + " in "
                            + month));

    AnomalyExplanation explanation =
        anomalyChatClient
            .prompt()
            .user(explanationPrompt(detected))
            .call()
            .entity(AnomalyExplanation.class);

    entity.setExplanation(explanation.explanation());
    AnomalyExplanationEntity saved = anomalyExplanationRepository.save(entity);

    return new AnomalyResponse(
        saved.getId(),
        detected.categoryId(),
        detected.categoryName(),
        detected.month(),
        detected.spend(),
        detected.meanSpend(),
        detected.zScore(),
        detected.iqrFlag(),
        saved.getExplanation());
  }

  private String explanationPrompt(DetectedAnomaly detected) {
    return "Category: "
        + detected.categoryName()
        + "\nMonth: "
        + detected.month()
        + "\nActual spend: "
        + detected.spend()
        + "\nHistorical average spend: "
        + detected.meanSpend()
        + "\nZ-score: "
        + detected.zScore()
        + "\nIQR outlier: "
        + detected.iqrFlag();
  }
}
