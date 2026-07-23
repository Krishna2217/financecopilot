package com.microsoft.financecopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises {@link ExecutiveReportRepository} — including the {@code sections} JSONB column and the
 * {@code (report_year, report_month)} uniqueness that makes report generation idempotent — against
 * a real, Flyway-migrated database.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExecutiveReportRepositoryTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
    registry.add("spring.flyway.user", POSTGRES::getUsername);
    registry.add("spring.flyway.password", POSTGRES::getPassword);
  }

  @Autowired private ExecutiveReportRepository executiveReportRepository;

  @Test
  void savesAndFindsByYearAndMonthWithJsonSections() {
    ExecutiveReportEntity entity = new ExecutiveReportEntity();
    entity.setReportYear(2026);
    entity.setReportMonth(6);
    entity.setMarkdown("# Executive Summary\n\nAll good.");
    entity.setSections("[{\"title\":\"Summary\",\"content\":\"All good.\"}]");
    entity.setCreatedAt(Instant.now());
    executiveReportRepository.save(entity);

    var found = executiveReportRepository.findByReportYearAndReportMonth(2026, 6);

    assertThat(found).isPresent();
    assertThat(found.get().getMarkdown()).isEqualTo("# Executive Summary\n\nAll good.");
    assertThat(found.get().getSections()).contains("Summary");
  }

  @Test
  void rejectsADuplicateReportForTheSameYearAndMonth() {
    ExecutiveReportEntity first = new ExecutiveReportEntity();
    first.setReportYear(2026);
    first.setReportMonth(7);
    first.setMarkdown("# First");
    first.setSections("[]");
    first.setCreatedAt(Instant.now());
    executiveReportRepository.saveAndFlush(first);

    ExecutiveReportEntity duplicate = new ExecutiveReportEntity();
    duplicate.setReportYear(2026);
    duplicate.setReportMonth(7);
    duplicate.setMarkdown("# Duplicate");
    duplicate.setSections("[]");
    duplicate.setCreatedAt(Instant.now());

    assertThatThrownBy(() -> executiveReportRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
