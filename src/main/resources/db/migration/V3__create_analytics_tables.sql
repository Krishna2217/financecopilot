CREATE TABLE analytics.accounts (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    account_type VARCHAR(30)  NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT')),
    institution  VARCHAR(120),
    currency     CHAR(3)      NOT NULL DEFAULT 'USD',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE analytics.categories (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL UNIQUE,
    parent_category_id  BIGINT REFERENCES analytics.categories (id),
    is_income           BOOLEAN     NOT NULL DEFAULT false
);

CREATE TABLE analytics.transactions (
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT         NOT NULL REFERENCES analytics.accounts (id),
    category_id       BIGINT         NOT NULL REFERENCES analytics.categories (id),
    amount            NUMERIC(14, 2) NOT NULL,
    currency          CHAR(3)        NOT NULL DEFAULT 'USD',
    description       VARCHAR(255),
    merchant          VARCHAR(120),
    transaction_date  DATE           NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_account_id ON analytics.transactions (account_id);
CREATE INDEX idx_transactions_category_id ON analytics.transactions (category_id);
CREATE INDEX idx_transactions_transaction_date ON analytics.transactions (transaction_date);

CREATE TABLE analytics.budgets (
    id            BIGSERIAL PRIMARY KEY,
    category_id   BIGINT         NOT NULL REFERENCES analytics.categories (id),
    budget_month  DATE           NOT NULL,
    amount_limit  NUMERIC(14, 2) NOT NULL,
    UNIQUE (category_id, budget_month)
);

CREATE TABLE analytics.monthly_summary (
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT         NOT NULL REFERENCES analytics.accounts (id),
    category_id        BIGINT         NOT NULL REFERENCES analytics.categories (id),
    summary_month      DATE           NOT NULL,
    total_amount       NUMERIC(14, 2) NOT NULL,
    transaction_count  INT            NOT NULL,
    UNIQUE (account_id, category_id, summary_month)
);
