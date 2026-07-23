package com.microsoft.financecopilot.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises {@link QueryHistoryRepository} (and the {@link QueryHistory} entity mapping, including
 * the Postgres {@code text[]} {@code tables_used} column) against a real database, migrated with
 * the project's actual Flyway scripts.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QueryHistoryRepositoryTest {

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

  @Autowired private QueryHistoryRepository queryHistoryRepository;

  @Test
  void savesAndRetrievesQueryHistoryMostRecentFirst() {
    QueryHistory older = new QueryHistory();
    older.setNlQuery("How much did I spend last month?");
    older.setGeneratedSql("SELECT sum(amount) FROM analytics.transactions LIMIT 1000");
    older.setTablesUsed(new String[] {"transactions"});
    older.setCreatedAt(Instant.now().minusSeconds(60));
    queryHistoryRepository.save(older);

    QueryHistory newer = new QueryHistory();
    newer.setNlQuery("What are my top categories?");
    newer.setGeneratedSql("SELECT category_id FROM analytics.transactions LIMIT 1000");
    newer.setTablesUsed(new String[] {"transactions", "categories"});
    newer.setCreatedAt(Instant.now());
    queryHistoryRepository.save(newer);

    var page = queryHistoryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getContent().get(0).getNlQuery()).isEqualTo("What are my top categories?");
    assertThat(page.getContent().get(1).getNlQuery()).isEqualTo("How much did I spend last month?");
    assertThat(page.getContent().get(0).getTablesUsed())
        .containsExactly("transactions", "categories");
  }
}
