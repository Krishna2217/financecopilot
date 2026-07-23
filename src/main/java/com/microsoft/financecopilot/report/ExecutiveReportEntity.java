package com.microsoft.financecopilot.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "executive_report", schema = "app")
public class ExecutiveReportEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "report_year", nullable = false)
  private int reportYear;

  @Column(name = "report_month", nullable = false)
  private int reportMonth;

  @Column(nullable = false)
  private String markdown;

  /** Raw JSON array of {@code ReportSection} objects, serialized/deserialized by the service. */
  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String sections;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public int getReportYear() {
    return reportYear;
  }

  public void setReportYear(int reportYear) {
    this.reportYear = reportYear;
  }

  public int getReportMonth() {
    return reportMonth;
  }

  public void setReportMonth(int reportMonth) {
    this.reportMonth = reportMonth;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }

  public String getSections() {
    return sections;
  }

  public void setSections(String sections) {
    this.sections = sections;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
