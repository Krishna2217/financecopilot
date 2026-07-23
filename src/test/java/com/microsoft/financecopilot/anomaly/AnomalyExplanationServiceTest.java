package com.microsoft.financecopilot.anomaly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.financecopilot.ai.dto.AnomalyExplanation;
import com.microsoft.financecopilot.anomaly.dto.DetectedAnomaly;
import com.microsoft.financecopilot.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class AnomalyExplanationServiceTest {

  @Mock private AnomalyDetector anomalyDetector;
  @Mock private AnomalyExplanationRepository anomalyExplanationRepository;

  private ChatClient anomalyChatClient;
  private ChatClient.ChatClientRequestSpec requestSpec;
  private ChatClient.CallResponseSpec callSpec;

  private AnomalyExplanationService service;

  @BeforeEach
  void setUp() {
    anomalyChatClient = mock(ChatClient.class);
    requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    callSpec = mock(ChatClient.CallResponseSpec.class);

    service =
        new AnomalyExplanationService(
            anomalyChatClient, anomalyDetector, anomalyExplanationRepository);
  }

  @Test
  void explainsAPreviouslyDetectedAnomalyAndPersistsTheExplanation() {
    AnomalyExplanationEntity entity = new AnomalyExplanationEntity();
    entity.setCategoryId(1L);
    entity.setSummaryMonth(LocalDate.of(2026, 6, 1));
    entity.setZScore(BigDecimal.valueOf(12.65));
    entity.setIqrFlag(true);
    entity.setCreatedAt(Instant.now());
    when(anomalyExplanationRepository.findById(7L)).thenReturn(Optional.of(entity));

    DetectedAnomaly detected =
        new DetectedAnomaly(
            1L,
            "Utilities",
            YearMonth.of(2026, 6),
            BigDecimal.valueOf(200),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(12.65),
            true);
    when(anomalyDetector.detectForCategory(1L, YearMonth.of(2026, 6)))
        .thenReturn(Optional.of(detected));

    when(anomalyChatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callSpec);
    when(callSpec.entity(AnomalyExplanation.class))
        .thenReturn(
            new AnomalyExplanation("Your utilities spend doubled compared to your usual average."));

    when(anomalyExplanationRepository.save(entity)).thenReturn(entity);

    var response = service.explain(7L);

    assertThat(response.categoryName()).isEqualTo("Utilities");
    assertThat(response.explanation())
        .isEqualTo("Your utilities spend doubled compared to your usual average.");
    assertThat(response.zScore()).isEqualByComparingTo("12.65");
    assertThat(response.iqrFlag()).isTrue();

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    verify(requestSpec).user(promptCaptor.capture());
    assertThat(promptCaptor.getValue()).contains("Utilities", "200", "100", "12.65", "true");

    assertThat(entity.getExplanation())
        .isEqualTo("Your utilities spend doubled compared to your usual average.");
  }

  @Test
  void throwsNotFoundWhenTheAnomalyIdDoesNotExist() {
    when(anomalyExplanationRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.explain(999L)).isInstanceOf(ResourceNotFoundException.class);

    verify(anomalyChatClient, never()).prompt();
    verify(anomalyExplanationRepository, never()).save(any());
  }

  @Test
  void throwsNotFoundWhenNoSpendHistoryExistsForTheCategory() {
    AnomalyExplanationEntity entity = new AnomalyExplanationEntity();
    entity.setCategoryId(1L);
    entity.setSummaryMonth(LocalDate.of(2026, 6, 1));
    when(anomalyExplanationRepository.findById(7L)).thenReturn(Optional.of(entity));
    when(anomalyDetector.detectForCategory(1L, YearMonth.of(2026, 6))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.explain(7L)).isInstanceOf(ResourceNotFoundException.class);

    verify(anomalyChatClient, never()).prompt();
    verify(anomalyExplanationRepository, never()).save(any());
  }
}
