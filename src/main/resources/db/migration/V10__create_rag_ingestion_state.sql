-- Tracks the content hash of each ingested RAG document (schema table docs, glossary chunks,
-- few-shot examples) so SchemaIngestionService can skip re-embedding unchanged documents on
-- every restart.
CREATE TABLE app.rag_ingestion_state (
    doc_id        TEXT PRIMARY KEY,
    content_hash  TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
