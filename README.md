# ControlLeads

Lead management system (CRM/funnel) for international student recruitment — single institution, entirely in English.

| Directory | What it is |
|---|---|
| `backend/` | Spring Boot 4 (Java 25) + PostgreSQL — all business logic |
| `web/` | Angular 22 (signals) + ECharts — web client |
| `app/` | Flutter — iOS/Android client, same APIs |
| `shared/api-contract/` | OpenAPI contract (exported from springdoc) → generated TS/Dart clients |
| `shared/design-tokens/` | `tokens.json` — visual source of truth for both clients |
| `.spec/` | Product discovery & specs (SDD) |

## Quick start

```bash
# 1. Database
docker compose up -d db

# 2. Backend (http://localhost:8090 — Swagger UI at /swagger-ui.html)
cd backend && ./mvnw spring-boot:run

# 3. Web (http://localhost:4200, proxies /api to :8090)
cd web && npm install && npm start

# 4. App
cd app && flutter run
```

## Shared-source pipelines

```bash
# Design tokens → SCSS (web) + Dart (app)
node shared/design-tokens/build.mjs

# API contract → generated clients (backend must be running)
./scripts/sync-api-contract.sh
```

## Notes

- Postgres is exposed on host port **5434**, backend on **8090** (5432/5433 and 8080/8081 are taken by other services on this machine).
- Backend tests use **Testcontainers** (need Docker up). Works with Colima out of the box — the socket override is set in `backend/pom.xml`.

Read `CLAUDE.md` for architecture rules and `.spec/discovery/roadmap.md` for the build order.
