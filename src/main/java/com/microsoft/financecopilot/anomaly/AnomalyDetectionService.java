package com.microsoft.financecopilot.anomaly;

import com.microsoft.financecopilot.anomaly.dto.AnomalyResponse;
import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Detects statistical anomalies and persists them into {@code app.anomaly_explanation} (without an
 * explanation yet) so they have a stable id for {@link AnomalyExplanationService} to later explain.
 * No AI involved — always available regardless of {@code app.ai.enabled}.
 */
@Service
public class AnomalyDetectionService {

  private final AnomalyDetector anomalyDetector;
  private final AnomalyExplanationRepository anomalyExplanationRepository;

  public AnomalyDetectionService(
      AnomalyDetector anomalyDetector, AnomalyExplanationRepository anomalyExplanationRepository) {
    this.anomalyDetector = anomalyDetector;
    this.anomalyExplanationRepository = anomalyExplanationRepository;
  }

  public List<AnomalyResponse> getAnomalies(YearMonth month) {
    return anomalyDetector.detect(month).stream().map(this::upsertAndBuildResponse).toList();
  }

  private AnomalyResponse upsertAndBuildResponse(DetectedAnomaly detected) {
    AnomalyExplanationEntity entity =
        anomalyExplanationRepository
            .findByCategoryIdAndSummaryMonth(detected.categoryId(), detected.month().atDay(1))
            .orElseGet(AnomalyExplanationEntity::new);
    entity.setCategoryId(detected.categoryId());
    entity.setSummaryMonth(detected.month().atDay(1));
    entity.setZScore(detected.zScore());
    entity.setIqrFlag(detected.iqrFlag());
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(Instant.now());
    }
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
}
