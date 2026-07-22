# FinanceCopilot

AI Finance Analytics Platform — natural-language querying, anomaly detection, and
executive reporting over financial data, built on Spring Boot 3 + Spring AI and a
React/TypeScript frontend.

## Stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL 16 + pgvector, Flyway,
  Actuator, springdoc-openapi
- **AI:** Spring AI 1.0.x (`ChatClient`, structured output, `PgVectorStore`, advisors)
- **Frontend:** React 18 + Vite + TypeScript, TanStack Query, Tailwind, Recharts
- **Ops:** Docker Compose, Azure App Service, Application Insights, GitHub Actions

## Local development

```bash
cp .env.example .env
docker compose up --build
```

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Building without Docker

```bash
./mvnw verify
./mvnw spring-boot:run
```

## Project status

Phase 0 (bootstrap) complete. See `CLAUDE.md` for the full phase plan.
