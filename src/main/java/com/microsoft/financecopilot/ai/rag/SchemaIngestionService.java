package com.microsoft.financecopilot.ai.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ingests the RAG corpus (per-table schema docs, glossary sections, NL→SQL few-shots) into the
 * {@link VectorStore} on startup. Idempotent: each document's SHA-256 content hash is compared
 * against {@code app.rag_ingestion_state}, and only changed/new documents are re-embedded, so a
 * restart with an unchanged corpus is a no-op.
 *
 * <p>Gated by {@code app.ai.enabled} — like {@code ChatClientConfig}, this needs an {@code
 * EmbeddingModel}/{@code VectorStore}, which don't exist without Azure/OpenAI credentials.
 */
@Component
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchemaIngestionService {

  private static final Logger log = LoggerFactory.getLogger(SchemaIngestionService.class);

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;
  private final SchemaDocumentBuilder schemaDocumentBuilder;
  private final GlossaryLoader glossaryLoader;
  private final NlSqlFewShotLoader fewShotLoader;

  public SchemaIngestionService(
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      SchemaDocumentBuilder schemaDocumentBuilder,
      GlossaryLoader glossaryLoader,
      NlSqlFewShotLoader fewShotLoader) {
    this.vectorStore = vectorStore;
    this.jdbcTemplate = jdbcTemplate;
    this.schemaDocumentBuilder = schemaDocumentBuilder;
    this.glossaryLoader = glossaryLoader;
    this.fewShotLoader = fewShotLoader;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ingest() {
    List<Document> documents = new ArrayList<>();
    documents.addAll(schemaDocumentBuilder.buildTableDocuments());
    documents.addAll(glossaryLoader.load());
    documents.addAll(fewShotLoader.load());

    int changed = 0;
    for (Document document : documents) {
      if (ingestIfChanged(document)) {
        changed++;
      }
    }
    log.info(
        "RAG ingestion complete: {} documents considered, {} embedded/updated, {} unchanged",
        documents.size(),
        changed,
        documents.size() - changed);
  }

  /** Returns true if the document was (re-)embedded, false if it was already up to date. */
  private boolean ingestIfChanged(Document document) {
    String hash = sha256(document.getText());
    String existingHash = fetchExistingHash(document.getId());
    if (hash.equals(existingHash)) {
      return false;
    }
    vectorStore.delete(List.of(document.getId()));
    vectorStore.add(List.of(document));
    upsertState(document.getId(), hash);
    return true;
  }

  private String fetchExistingHash(String docId) {
    List<String> hashes =
        jdbcTemplate.query(
            "SELECT content_hash FROM app.rag_ingestion_state WHERE doc_id = ?",
            (rs, rowNum) -> rs.getString(1),
            docId);
    return hashes.isEmpty() ? null : hashes.get(0);
  }

  private void upsertState(String docId, String hash) {
    jdbcTemplate.update(
        "INSERT INTO app.rag_ingestion_state (doc_id, content_hash, updated_at) "
            + "VALUES (?, ?, now()) "
            + "ON CONFLICT (doc_id) DO UPDATE SET content_hash = EXCLUDED.content_hash, updated_at = now()",
        docId,
        hash);
  }

  private String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
