-- Mirrors the schema Spring AI's PgVectorStore provisions via
-- spring.ai.vectorstore.pgvector.initialize-schema=true (default id type, table name,
-- and OpenAI embedding dimension of 1536), so that later phases' auto-provisioning
-- is a no-op against this table.
CREATE TABLE IF NOT EXISTS public.vector_store (
    id        uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    content   text,
    metadata  json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index
    ON public.vector_store USING hnsw (embedding vector_cosine_ops);
