package com.microsoft.financecopilot.anomaly;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyExplanationRepository
    extends JpaRepository<AnomalyExplanationEntity, Long> {

  Optional<AnomalyExplanationEntity> findByCategoryIdAndSummaryMonth(
      Long categoryId, LocalDate summaryMonth);
}
