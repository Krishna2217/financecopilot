package com.microsoft.financecopilot.anomaly;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "anomaly_explanation", schema = "app")
public class AnomalyExplanationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "category_id", nullable = false)
  private Long categoryId;

  @Column(name = "summary_month", nullable = false)
  private LocalDate summaryMonth;

  @Column(name = "z_score")
  private BigDecimal zScore;

  @Column(name = "iqr_flag", nullable = false)
  private boolean iqrFlag;

  private String explanation;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public LocalDate getSummaryMonth() {
    return summaryMonth;
  }

  public void setSummaryMonth(LocalDate summaryMonth) {
    this.summaryMonth = summaryMonth;
  }

  public BigDecimal getZScore() {
    return zScore;
  }

  public void setZScore(BigDecimal zScore) {
    this.zScore = zScore;
  }

  public boolean isIqrFlag() {
    return iqrFlag;
  }

  public void setIqrFlag(boolean iqrFlag) {
    this.iqrFlag = iqrFlag;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
