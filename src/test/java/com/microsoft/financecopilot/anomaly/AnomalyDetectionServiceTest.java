package com.microsoft.financecopilot.anomaly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

  @Mock private AnomalyDetector anomalyDetector;
  @Mock private AnomalyExplanationRepository anomalyExplanationRepository;

  private AnomalyDetectionService service;

  @BeforeEach
  void setUp() {
    service = new AnomalyDetectionService(anomalyDetector, anomalyExplanationRepository);
  }

  @Test
  void persistsNewlyDetectedAnomaliesAndReturnsThem() {
    YearMonth month = YearMonth.of(2026, 6);
    DetectedAnomaly detected =
        new DetectedAnomaly(
            1L,
            "Utilities",
            month,
            BigDecimal.valueOf(200),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(12.65),
            true);
    when(anomalyDetector.detect(month)).thenReturn(List.of(detected));
    when(anomalyExplanationRepository.findByCategoryIdAndSummaryMonth(1L, LocalDate.of(2026, 6, 1)))
        .thenReturn(Optional.empty());
    when(anomalyExplanationRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              AnomalyExplanationEntity entity = invocation.getArgument(0);
              org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", 7L);
              return entity;
            });

    var responses = service.getAnomalies(month);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(7L);
    assertThat(responses.get(0).categoryName()).isEqualTo("Utilities");
    assertThat(responses.get(0).explanation()).isNull();
  }

  @Test
  void reusesTheExistingRowAndPreservesAnyPriorExplanationForAnAlreadySeenAnomaly() {
    YearMonth month = YearMonth.of(2026, 6);
    DetectedAnomaly detected =
        new DetectedAnomaly(
            1L,
            "Utilities",
            month,
            BigDecimal.valueOf(200),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(12.65),
            true);
    when(anomalyDetector.detect(month)).thenReturn(List.of(detected));

    AnomalyExplanationEntity existing = new AnomalyExplanationEntity();
    existing.setCategoryId(1L);
    existing.setSummaryMonth(LocalDate.of(2026, 6, 1));
    existing.setExplanation("Already explained.");
    org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 5L);
    when(anomalyExplanationRepository.findByCategoryIdAndSummaryMonth(1L, LocalDate.of(2026, 6, 1)))
        .thenReturn(Optional.of(existing));
    when(anomalyExplanationRepository.save(existing)).thenReturn(existing);

    var responses = service.getAnomalies(month);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(5L);
    assertThat(responses.get(0).explanation()).isEqualTo("Already explained.");
  }
}
