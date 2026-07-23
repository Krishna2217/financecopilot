# FinanceCopilot

[![Backend CI](https://github.com/Krishna2217/financecopilot/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/Krishna2217/financecopilot/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/Krishna2217/financecopilot/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/Krishna2217/financecopilot/actions/workflows/frontend-ci.yml)

AI Finance Analytics Platform — natural-language querying, anomaly detection, and
executive reporting over financial data, built on Spring Boot 3 + Spring AI and a
React/TypeScript frontend.

## Contents

- [Features](#features)
- [Stack](#stack)
- [Quick start](#quick-start)
- [Enabling AI chat](#enabling-ai-chat)
- [Building without Docker](#building-without-docker)
- [Architecture](#architecture)
- [API reference](#api-reference)
- [Environment variables](#environment-variables)
- [Runbook](#runbook)
- [Threat model](#threat-model)
- [Project status](#project-status)

## Features

- **Dashboard** — KPI summary (income, expenses, net cashflow, savings rate), spend-by-
  category and cashflow-trend charts, all served by parameterized SQL (no AI involved, so
  it works with zero LLM credentials).
- **Chat (NL→SQL)** — ask a question in plain English, get back the generated SQL, the
  model's rationale, the query result table, and a plain-language summary. Grounded in the
  live database schema and a business glossary via retrieval-augmented generation (RAG).
- **Anomalies** — statistical (z-score + IQR) detection of unusual monthly spend per
  category, with an optional AI-generated explanation for any flagged anomaly.
- **Executive reports** — a generated markdown report combining KPIs and top anomalies,
  idempotent per `{year, month}`.
- **History** — paginated log of every NL query run, with token/latency metrics and
  one-click re-run.

## Stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL 16 + pgvector, Flyway,
  Actuator, springdoc-openapi
- **AI:** Spring AI 1.0.x (`ChatClient`, structured output, `PgVectorStore`, advisors) —
  Azure OpenAI or plain OpenAI as the model provider
- **Frontend:** React 18 + Vite + TypeScript, TanStack Query, Tailwind, Recharts
- **Testing:** JUnit 5, Mockito, Testcontainers (`pgvector/pgvector:pg17`), JaCoCo (≥75%
  service-layer coverage gate)
- **Ops:** Docker Compose, Azure Container Registry, Azure App Service, Azure Static Web
  Apps, Application Insights, GitHub Actions

## Quick start

```bash
cp .env.example .env
docker compose up --build
```

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

This boots on the `local` profile with AI disabled — no API key needed. Dashboard,
Anomalies, and History all work; Chat and "Explain" return 404 until AI is enabled (see
below).

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173, proxies /api to localhost:8080
```

## Enabling AI chat

Chat (NL→SQL), anomaly explanations, and executive reports need a real model provider. Two
options, both configured in `.env`:

**Plain OpenAI** (`api.openai.com`):
```bash
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=gpt-4o-mini   # optional, this is the default
SPRING_PROFILES_ACTIVE=local,openai
```

**Azure OpenAI**:
```bash
AZURE_OPENAI_ENDPOINT=https://<resource>.openai.azure.com
AZURE_OPENAI_API_KEY=...
AZURE_OPENAI_CHAT_DEPLOYMENT=<your-deployment-name>
SPRING_PROFILES_ACTIVE=dev
```

Then `docker compose up -d --build app` to pick up the new profile/env. The `openai`
profile also drives schema RAG ingestion (embeddings) through the same OpenAI account — an
account with no billing/credits will fail with `insufficient_quota` on both chat and
embedding calls, and that failure currently crashes the app at startup rather than
degrading gracefully (a known gap in `SchemaIngestionService`, not specific to either
provider).

## Building without Docker

```bash
./mvnw verify
./mvnw spring-boot:run
```

## Architecture

```
                         ┌─────────────────────────┐
                         │  Azure Static Web Apps   │
  Browser ───────────────▶  (React/Vite frontend)   │
                         └────────────┬─────────────┘
                                      │ /api/* (HTTPS)
                                      ▼
                         ┌─────────────────────────┐
                         │     Azure App Service     │
                         │  (Spring Boot container,  │
                         │   pulled from ACR)         │
                         │                            │
                         │  controller → service →    │
                         │  repository                │
                         │                            │
                         │  ai/  ── ChatClient beans  │──────▶ Azure OpenAI
                         │         (nl2sql, anomaly,  │        (or OpenAI)
                         │          report)            │
                         │  sql/  ── SqlSafetyValidator│
                         │         + SqlExecutor        │
                         └────────────┬────────────────┘
                                      │ finance_ro (SELECT-only, analytics.*)
                                      │ finance_app (read/write, app.*)
                                      ▼
                         ┌─────────────────────────┐
                         │ PostgreSQL 16 + pgvector  │
                         │ analytics.*  app.*        │
                         │ vector_store (schema RAG) │
                         └─────────────────────────┘

                         Application Insights ◀──── Micrometer + Java agent
                         (traces, ai.tokens.*, sql.exec.ms, sql.rejected.count)
```

Two GitHub Actions CI workflows (`backend-ci.yml`, `frontend-ci.yml`) run `mvn verify` /
`npm run build` on every push and PR to `main`. A `deploy.yml` workflow, gated on those
succeeding, builds the backend image, pushes it to ACR, rolls it out to App Service, and
publishes the frontend build to Static Web Apps. See
[`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) for the exact steps and the
secrets it requires — those Azure resources are not yet provisioned for this repository, so
the deploy workflow's jobs will fail at the Azure login step until they exist.

## API reference

Full request/response examples are in Swagger UI (`/swagger-ui.html`) and the OpenAPI doc
(`/v3/api-docs`). Summary:

| Endpoint | AI? | Description |
| --- | --- | --- |
| `POST /api/v1/query` | Yes | NL question → generated SQL → executed result → summary |
| `GET /api/v1/query/history` | No | Paginated query history |
| `GET /api/v1/analytics/kpis` | No | Income/expenses/net cashflow/savings rate for a month |
| `GET /api/v1/analytics/spend-by-category` | No | Spend breakdown by category |
| `GET /api/v1/analytics/cashflow` | No | Monthly cashflow trend |
| `GET /api/v1/analytics/trend` | No | Spend trend, optionally filtered by category |
| `GET /api/v1/anomalies` | No | Z-score + IQR anomaly detection for a month |
| `POST /api/v1/anomalies/{id}/explain` | Yes | AI explanation for a detected anomaly |
| `POST /api/v1/reports/executive` | Yes | Generated executive markdown report |

Endpoints marked "AI? Yes" require `app.ai.enabled=true` (see
[Enabling AI chat](#enabling-ai-chat)) and 404 otherwise.

## Environment variables

| Variable | Used by | Description |
| --- | --- | --- |
| `DB_URL` | app, Flyway | JDBC URL of the Postgres instance, e.g. `jdbc:postgresql://db:5432/financecopilot` |
| `DB_SUPERUSER` / `DB_SUPERUSER_PASSWORD` | Flyway | Elevated credentials used only for migrations, which create the `finance_ro`/`finance_app` roles themselves |
| `DB_APP_USERNAME` / `DB_APP_PASSWORD` | app runtime | Least-privilege `finance_app` role — read/write on `app.*` only |
| `DB_RO_USERNAME` / `DB_RO_PASSWORD` | `SqlExecutor` | `finance_ro` role — `SELECT`-only on `analytics.*`, the only role that ever runs AI-generated SQL |
| `AZURE_OPENAI_ENDPOINT` / `AZURE_OPENAI_API_KEY` / `AZURE_OPENAI_CHAT_DEPLOYMENT` | `ChatClientConfig` (`dev`/`prod` profiles) | Azure OpenAI provider |
| `OPENAI_API_KEY` / `OPENAI_CHAT_MODEL` | `ChatClientConfig` (`openai` profile) | Plain OpenAI provider; model defaults to `gpt-4o-mini` |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | App Insights Java agent | Self-disables gracefully (no telemetry, no crash) if unset |
| `SPRING_PROFILES_ACTIVE` | Spring Boot | `local` (AI disabled) \| `local,openai` \| `dev` (Azure OpenAI) \| `prod` |

GitHub Actions secrets required only by `deploy.yml` (not consumed by the app itself):
`AZURE_CREDENTIALS`, `ACR_NAME`, `AZURE_WEBAPP_NAME`, `AZURE_RESOURCE_GROUP`,
`AZURE_STATIC_WEB_APPS_API_TOKEN`.

## Runbook

**Health checks**
- `GET /actuator/health` — overall app health, including datasource connectivity
- `GET /actuator/metrics` — `sql.exec.ms`, `sql.rejected.count`, `query.errors.count`,
  `ai.tokens.{prompt,completion}`, plus Spring AI's built-in observations
  (`spring.ai.chat.client`, `spring.ai.chat.model`, `spring.ai.vector.store`)
- Application Insights: end-to-end traces for chat calls, tagged with `use_case` and `model`

**Common incidents**
- *App won't start / Flyway fails*: check `DB_SUPERUSER`/`DB_SUPERUSER_PASSWORD` first —
  migrations create the `finance_ro`/`finance_app` roles, so Flyway needs elevated creds
  distinct from the app's own runtime datasource.
- *`finance_app` password auth failures on a fresh database*: Hikari's eager connection
  validation can race Flyway's role-creation migration; `PrimaryDataSourceConfig` sets
  `initializationFailTimeout(-1)` to defer it — if this regresses, that's the first thing to
  check.
- *App crashes at startup with an AI profile enabled*: check for `insufficient_quota` or
  other embedding-call failures from `SchemaIngestionService` in the logs — it currently
  propagates any embedding failure into a full application-context failure instead of
  logging and continuing. Fall back to the `local` profile to get a healthy app while you
  fix the provider-side issue (billing, quota, network).
- *AI endpoints (`/api/v1/query`, anomaly `/explain`, `/api/v1/reports/executive`) 404*:
  `app.ai.enabled` is false — this is the default in `local` with no Azure/OpenAI
  credentials. Confirmed by design; not a bug. See [Enabling AI chat](#enabling-ai-chat).
- *`sql.rejected.count` climbing*: check `SqlSafetyValidator` logs for the rejection reason
  (non-SELECT, disallowed table, missing `LIMIT`, etc.) before assuming an attack — the model
  can also just generate a bad query.
- *Elevated `query.errors.count` / 429s from the model*: check the configured retry/timeout
  `@ConfigurationProperties` for the AI use case and the upstream provider's quota/billing.

**Rollback**
- App Service: redeploy the previous image tag from ACR (`docker push` tags images by
  commit SHA in `deploy.yml`, so any prior SHA can be redeployed via
  `az webapp config container set --container-image-name <acr>.azurecr.io/financecopilot:<sha>`).
- Static Web Apps: re-run the `deploy-frontend` job against the previous commit, or revert
  the frontend change and push to `main`.
- Database: Flyway migrations are additive/forward-only in this project; there are no down
  migrations. A schema rollback means restoring from a Postgres backup/snapshot.

## Threat model

**SQL generation surface** (`POST /api/v1/query`, anomaly `/explain`, executive reports) —
the highest-risk path, since it turns free-text user input into SQL executed against real
data.

- *Threat*: prompt injection or an adversarial NL query coaxes the model into generating
  destructive or exfiltrating SQL (`DROP TABLE`, `UNION`-based cross-tenant reads, comment-
  hidden statements, multi-statement payloads).
- *Mitigations*:
  - `PromptGuardAdvisor` rejects prompts containing DDL/DML tokens or obvious injection
    patterns before they reach the model.
  - The model only ever returns a structured `GeneratedSql` record via Spring AI's
    `.entity(...)` — never raw text parsed with regex.
  - `SqlSafetyValidator` runs in the service layer (not as an advisor, deliberately, so it
    can't be bypassed by advisor ordering) and: parses with JSqlParser and rejects anything
    but exactly one `SELECT`; rejects DDL/DML/COPY/CALL/GRANT/REVOKE/TRUNCATE tokens,
    comment-hidden statements, and multi-statement input; enforces a table allowlist from
    live schema introspection; enforces `LIMIT ≤ 1000`; runs under
    `SET LOCAL statement_timeout = '5s'`.
  - The `finance_ro` database role that executes all AI-generated SQL has `SELECT`-only
    grants on the `analytics` schema and no access to `app.*` at all — even a validator
    bypass can't reach query history, anomaly explanations, or other tenants' write paths.
  - Raw user data is never sent into prompts; the RAG context is schema metadata and a
    curated glossary, not row contents.
- *Residual risk*: a sufficiently novel SQL construct could theoretically slip past
  JSqlParser's grammar or the allowlist; the `finance_ro` grant boundary is the backstop if
  it does. Adversarial test coverage (15+ cases) lives in `SqlSafetyValidatorTest`.

**Prompt surface** (system prompts, RAG context, user input to `ChatClient`) —

- *Threat*: leaking the system prompt, few-shot examples, or glossary content; a user
  steering the model into ignoring its guardrails via injected instructions in the NL query.
- *Mitigations*:
  - System prompts and few-shots live in versioned `.st` templates
    (`src/main/resources/prompts/`), never inlined or built from user input.
  - `PromptGuardAdvisor` runs before the model call and rejects known injection patterns.
  - `TokenUsageAdvisor` and Application Insights tracing make unusual prompt/response volume
    (a possible sign of extraction attempts) observable via `ai.tokens.*` metrics.
  - Structured output (`.entity(...)`) means the service only ever consumes fields it
    explicitly modeled — the model can't smuggle extra behavior through free-text a
    downstream system would `eval` or otherwise trust.
- *Residual risk*: no output-side redaction currently inspects `.entity(...)` field values
  for leaked system-prompt content; if `RedactionAdvisor` (present in the package layout as
  a placeholder) is ever implemented, this is the reason.

**Secrets and credentials** — all secrets come from environment variables (`.env`, not
committed) or GitHub Actions secrets; only `.env.example` (blank values) is checked in. The
`finance_ro`/`finance_app` split means a compromised app-tier credential still can't reach
the other tier's grants.

## Project status

All 10 planned phases are complete: backend (0-8), frontend (9), and CI/CD + production
docs (10). See `CLAUDE.md` for the full phase-by-phase history.

Two things remain open, tracked here rather than as a phase:
- **Azure deploy is unwired** — `deploy.yml` exists and is correct, but no ACR/App
  Service/Static Web Apps resources are provisioned yet, so it fails at the Azure login
  step. Deployed URLs will be added here once that's done.
- **`SchemaIngestionService` isn't resilient to embedding failures** — an `insufficient_quota`
  or transient network error during startup RAG ingestion currently crashes the whole app
  instead of logging a warning and continuing without RAG grounding.
