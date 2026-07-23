CREATE TABLE app.executive_report (
    id            BIGSERIAL PRIMARY KEY,
    report_year   INT         NOT NULL,
    report_month  INT         NOT NULL CHECK (report_month BETWEEN 1 AND 12),
    markdown      TEXT        NOT NULL,
    sections      JSONB       NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (report_year, report_month)
);
