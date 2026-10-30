# Copilot instructions

## Project shape

Fluxis AI is a single-repository web platform. The intended MVP is a modular monolith: a React/TypeScript browser client calls a Spring Boot REST API, which owns business rules, authentication and authorization, persistence, integrations, AI orchestration, approvals, and auditing. PostgreSQL stores application data and suggestion/action history. See [architecture.md](../docs/architecture.md).

The backend is organized by business capability (`auth`, `email`, `jobs`, `crm`, `services`, `proposals`, `subscriptions`, `tasks`, `ai`, `audit`, and `shared`). Keep module boundaries explicit: a module should use another module's internal API, not access its tables directly. Do not introduce microservices or Kubernetes for the MVP.

The frontend owns presentation, forms, UX validation, loading/error states, and the approval interface; it is not the authority for business rules or action completion. Backend confirmation is required before the UI presents an action as complete.

## Product and data constraints

- AI proposes; the user decides. Never send email, move messages, delete information, or perform another sensitive external action without explicit user approval. Approval must apply to a specific, idempotent action; the backend validates and executes it.
- Scope every user's data access by `user_id`. Keep Google OAuth tokens in the backend, encrypted at rest; never put tokens or secrets in the repository or browser `localStorage`. Request only the Gmail scopes needed.
- Treat email content as untrusted input, including in AI prompts.
- Follow [data-model.md](../docs/data-model.md) when implementing persistence: use `NUMERIC` plus an explicit currency for money, time-zone-aware timestamps for instants, Gmail external IDs (not subjects) as relationship keys, constrained states, and appropriate foreign keys, uniqueness constraints, and indexes in the initial migration.

## Stack and repository conventions

- Follow [ADR-0001](../docs/decisions/0001-stack-versions.md) for selected stack versions: Java 25, Spring Boot 4.1, Maven Wrapper, PostgreSQL 18, Node.js 24 (`.nvmrc`), React 19, strict TypeScript 6, Vite 8, and npm. Framework-managed backend dependency versions should come from the Spring Boot BOM unless an ADR justifies otherwise. Boot 4 / Spring AI 2 use Jackson 3; do not assume Boot 3 examples are compatible.
- Spring Boot is the default home for AI features. Add a Python/FastAPI service only for a documented, specific benefit; if added, use validated Pydantic inputs/outputs, internal authenticated HTTP, tests and Ruff linting, and do not give it Gmail credentials or sensitive action-execution authority. See [ADR-0002](../docs/decisions/0002-modular-monolith-and-python.md).
- Keep the monorepo's application code in `backend/` and `frontend/`; shared architecture, schema, roadmap, and technical decisions belong in `docs/`.
- Use `main` for stable releases, `dev` for integration, and `feat/...`, `fix/...`, or `docs/...` work branches. Use Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`); see [ADR-0003](../docs/decisions/0003-git-workflow.md).

## Build, test, and lint

There are no runnable project build, test, or lint commands yet: `backend/` and `frontend/` are placeholders, and the repository currently has no Maven project/wrapper, npm manifest, or CI workflow. Do not invent or report commands as available. Once these are added, use the commands defined by the actual Maven configuration and npm scripts, including their focused/single-test options, and update this section.
