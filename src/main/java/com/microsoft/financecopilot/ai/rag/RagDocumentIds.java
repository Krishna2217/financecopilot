package com.microsoft.financecopilot.ai.rag;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * {@code vector_store.id} is a {@code uuid} column (matching Spring AI's default schema, see the
 * Phase 1 migration), so a document's storage id can't be a human-readable string like {@code
 * "table:accounts"} directly. This derives a stable UUID from that logical key instead —
 * re-ingesting the same logical document always maps to the same row, which is what makes
 * delete-then-add idempotent update semantics work.
 */
final class RagDocumentIds {

  private RagDocumentIds() {}

  static String deterministicId(String logicalKey) {
    return UUID.nameUUIDFromBytes(logicalKey.getBytes(StandardCharsets.UTF_8)).toString();
  }
}
