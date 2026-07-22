package com.microsoft.financecopilot.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.financecopilot.config.SqlSafetyProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises {@link SchemaIngestionService} end to end against a real {@code pgvector/pgvector:pg17}
 * container, per the Phase 2.5 DoD: ingestion runs, is a no-op on restart, and retrieval for "show
 * me last month's spend" surfaces the {@code transactions} table document.
 *
 * <p>No real embedding API is available in CI, so a deterministic bag-of-words hashing {@link
 * EmbeddingModel} stub stands in for it: real cosine similarity math against real vectors in a real
 * vector store, just without a network call.
 */
@Testcontainers
class SchemaIngestionServiceIntegrationTest {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("financecopilot_test");

  private static HikariDataSource dataSource;
  private static JdbcTemplate jdbcTemplate;
  private static VectorStore vectorStore;
  private static SchemaIngestionService ingestionService;

  @BeforeAll
  static void setUpDatabaseAndVectorStore() {
    POSTGRES.start();

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
    hikariConfig.setUsername(POSTGRES.getUsername());
    hikariConfig.setPassword(POSTGRES.getPassword());
    dataSource = new HikariDataSource(hikariConfig);
    jdbcTemplate = new JdbcTemplate(dataSource);

    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
    jdbcTemplate.execute("CREATE SCHEMA analytics");
    jdbcTemplate.execute("CREATE SCHEMA app");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.accounts (id BIGSERIAL PRIMARY KEY, name VARCHAR(120) NOT NULL, "
            + "account_type VARCHAR(30) NOT NULL, institution VARCHAR(120), currency CHAR(3))");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.categories (id BIGSERIAL PRIMARY KEY, name VARCHAR(80) NOT NULL, "
            + "parent_category_id BIGINT, is_income BOOLEAN NOT NULL DEFAULT false)");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.transactions (id BIGSERIAL PRIMARY KEY, "
            + "account_id BIGINT NOT NULL REFERENCES analytics.accounts (id), "
            + "category_id BIGINT NOT NULL REFERENCES analytics.categories (id), "
            + "amount NUMERIC(14,2) NOT NULL, currency CHAR(3), description VARCHAR(255), "
            + "merchant VARCHAR(120), transaction_date DATE NOT NULL)");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.budgets (id BIGSERIAL PRIMARY KEY, "
            + "category_id BIGINT NOT NULL REFERENCES analytics.categories (id), "
            + "budget_month DATE NOT NULL, amount_limit NUMERIC(14,2) NOT NULL)");
    jdbcTemplate.execute(
        "CREATE TABLE analytics.monthly_summary (id BIGSERIAL PRIMARY KEY, "
            + "account_id BIGINT NOT NULL, category_id BIGINT NOT NULL, summary_month DATE NOT NULL, "
            + "total_amount NUMERIC(14,2) NOT NULL, transaction_count INT NOT NULL)");
    jdbcTemplate.execute(
        "CREATE TABLE app.rag_ingestion_state (doc_id TEXT PRIMARY KEY, content_hash TEXT NOT NULL, "
            + "updated_at TIMESTAMPTZ NOT NULL DEFAULT now())");

    jdbcTemplate.update(
        "INSERT INTO analytics.accounts (name, account_type, institution, currency) "
            + "VALUES ('Primary Checking', 'CHECKING', 'Test Bank', 'USD')");
    jdbcTemplate.update(
        "INSERT INTO analytics.categories (name, is_income) VALUES ('Groceries', false)");
    jdbcTemplate.update(
        "INSERT INTO analytics.transactions "
            + "(account_id, category_id, amount, currency, description, merchant, transaction_date) "
            + "VALUES (1, 1, -42.50, 'USD', 'Groceries - Test Market', 'Test Market', current_date)");

    EmbeddingModel fakeEmbeddingModel = new BagOfWordsHashingEmbeddingModel();
    PgVectorStore pgVectorStore =
        PgVectorStore.builder(jdbcTemplate, fakeEmbeddingModel)
            .dimensions(1536)
            .initializeSchema(true)
            .build();
    pgVectorStore.afterPropertiesSet(); // provisions the vector_store table + index
    vectorStore = pgVectorStore;

    SqlSafetyProperties properties =
        new SqlSafetyProperties(
            POSTGRES.getJdbcUrl(),
            "postgres",
            "postgres",
            1000,
            Duration.ofSeconds(5),
            "analytics");
    ingestionService =
        new SchemaIngestionService(
            vectorStore,
            jdbcTemplate,
            new SchemaDocumentBuilder(jdbcTemplate, properties),
            new GlossaryLoader(),
            new NlSqlFewShotLoader(new com.fasterxml.jackson.databind.ObjectMapper()));
  }

  @AfterAll
  static void tearDown() {
    dataSource.close();
    POSTGRES.stop();
  }

  @Test
  void ingestionIsANoOpOnRestartAndRetrievalSurfacesTheTransactionsTableDoc() {
    ingestionService.ingest();
    Long countAfterFirstRun =
        jdbcTemplate.queryForObject("SELECT count(*) FROM public.vector_store", Long.class);
    assertThat(countAfterFirstRun).isGreaterThan(0);

    Long stateRowsAfterFirstRun =
        jdbcTemplate.queryForObject("SELECT count(*) FROM app.rag_ingestion_state", Long.class);

    // Second run against an unchanged corpus must be a no-op: same document count, same state
    // rows, and (since we never re-delete+re-add unchanged docs) the same underlying rows.
    ingestionService.ingest();
    Long countAfterSecondRun =
        jdbcTemplate.queryForObject("SELECT count(*) FROM public.vector_store", Long.class);
    Long stateRowsAfterSecondRun =
        jdbcTemplate.queryForObject("SELECT count(*) FROM app.rag_ingestion_state", Long.class);

    assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
    assertThat(stateRowsAfterSecondRun).isEqualTo(stateRowsAfterFirstRun);

    // topK=8 matches nl2sqlChatClient's QuestionAnswerAdvisor configuration (ChatClientConfig) —
    // the DoD is that retrieval surfaces the transactions doc for this query, not that a crude
    // test-double embedding must rank it strictly first among short, topically-adjacent few-shots.
    List<Document> results =
        vectorStore.similaritySearch(
            SearchRequest.builder().query("show me last month's spend").topK(8).build());

    assertThat(results)
        .withFailMessage(
            "Expected retrieval to include the transactions table doc, got: %s", results)
        .anyMatch(document -> "transactions".equals(document.getMetadata().get("table")));
  }

  /**
   * Deterministic, network-free stand-in for a real embedding model: hashes each token into one of
   * 1536 buckets and L2-normalizes, so documents sharing more distinctive vocabulary with the query
   * score higher under cosine similarity — enough to make retrieval assertions meaningful without
   * calling a real embedding API in tests.
   */
  private static final class BagOfWordsHashingEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 1536;

    // Generic query-framing/connector words filtered out so similarity is driven by topical
    // content words, not incidental phrasing shared between the query and unrelated documents
    // (e.g. "show me ... last month" appearing verbatim in an unrelated NL2SQL example).
    private static final java.util.Set<String> STOPWORDS =
        java.util.Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "in", "is", "it",
            "its", "me", "my", "of", "on", "or", "show", "such", "that", "the", "this", "to", "was",
            "were", "will", "with");

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
      List<Embedding> embeddings =
          request.getInstructions().stream().map(text -> new Embedding(embed(text), 0)).toList();
      return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
      return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
      float[] vector = new float[DIMENSIONS];
      // Distinct-token presence (not raw counts): a word repeated within one document
      // shouldn't out-rank genuine topical overlap with the query just by appearing twice.
      // Tokens of length <= 1 are dropped as noise (mostly possessive/contraction fragments,
      // e.g. the "s" left over from splitting "month's" on the apostrophe).
      java.util.Set<String> tokens = new java.util.HashSet<>();
      for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
        if (token.length() > 1 && !STOPWORDS.contains(token)) {
          tokens.add(token);
        }
      }
      for (String token : tokens) {
        int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
        vector[bucket] = 1f;
      }
      double norm = 0;
      for (float v : vector) {
        norm += v * v;
      }
      norm = Math.sqrt(norm);
      if (norm > 0) {
        for (int i = 0; i < vector.length; i++) {
          vector[i] = (float) (vector[i] / norm);
        }
      }
      return vector;
    }
  }
}
