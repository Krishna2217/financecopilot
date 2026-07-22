CREATE TABLE app.query_history (
    id                 BIGSERIAL PRIMARY KEY,
    nl_query           TEXT        NOT NULL,
    generated_sql      TEXT        NOT NULL,
    rationale          TEXT,
    tables_used        TEXT[],
    rows_returned      INT,
    summary            TEXT,
    prompt_tokens      INT,
    completion_tokens  INT,
    sql_exec_ms        BIGINT,
    total_ms           BIGINT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_query_history_created_at ON app.query_history (created_at);

CREATE TABLE app.anomaly_explanation (
    id             BIGSERIAL PRIMARY KEY,
    category_id    BIGINT         NOT NULL,
    summary_month  DATE           NOT NULL,
    z_score        NUMERIC(10, 4),
    iqr_flag       BOOLEAN        NOT NULL DEFAULT false,
    explanation    TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (category_id, summary_month)
);
