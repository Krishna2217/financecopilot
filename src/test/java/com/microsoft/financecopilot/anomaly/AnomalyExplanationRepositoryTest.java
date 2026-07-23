package com.microsoft.financecopilot.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Exercises {@link AnomalyExplanationRepository} against a real, Flyway-migrated database. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnomalyExplanationRepositoryTest {

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

  @Autowired private AnomalyExplanationRepository anomalyExplanationRepository;

  @Test
  void savesAndFindsByCategoryAndMonthUniqueConstraint() {
    AnomalyExplanationEntity entity = new AnomalyExplanationEntity();
    entity.setCategoryId(1L);
    entity.setSummaryMonth(LocalDate.of(2026, 6, 1));
    entity.setZScore(BigDecimal.valueOf(12.65));
    entity.setIqrFlag(true);
    entity.setCreatedAt(Instant.now());
    AnomalyExplanationEntity saved = anomalyExplanationRepository.save(entity);

    var found =
        anomalyExplanationRepository.findByCategoryIdAndSummaryMonth(1L, LocalDate.of(2026, 6, 1));

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getZScore()).isEqualByComparingTo("12.65");
    assertThat(found.get().isIqrFlag()).isTrue();
    assertThat(found.get().getExplanation()).isNull();
  }

  @Test
  void findByCategoryAndMonthIsEmptyWhenNoRowExists() {
    var found =
        anomalyExplanationRepository.findByCategoryIdAndSummaryMonth(
            999L, LocalDate.of(2026, 6, 1));

    assertThat(found).isEmpty();
  }
}
