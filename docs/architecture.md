# Architecture

Modular monolith. No microservices or Kubernetes in the MVP.

```
 Browser (React + TS)
        │  HTTPS / JSON (REST API, OpenAPI)
        ▼
 Spring Boot ──────────► PostgreSQL 18
   │   │
   │   └──► Gmail API (OAuth 2.0, tokens encrypted in the backend)
   └──────► AI provider (Spring AI, configurable)

 (Optional, only if justified) Python/FastAPI service ◄── Internal HTTP from Spring Boot
```

## Responsibilities

- **Frontend:** presentation, forms, UX validation, loading/error states, approval panel.
- **Backend:** authentication, authorization, business rules, persistence, Gmail integration, approvals, auditing, AI orchestration, and controlled action execution.
- **PostgreSQL:** application data, pending suggestions, and history.

## Backend modules (packages)

`auth`, `email`, `jobs`, `crm`, `services`, `proposals`, `subscriptions`, `tasks`, `ai`, `audit`, `shared`.

Each module exposes a clear internal API and does not access another module's tables directly.

## Cross-cutting rules

1. The AI proposes; the user decides. No sensitive external action without approval.
2. Approval is tied to a specific, idempotent action.
3. Data is isolated by user (`user_id` in every query).
4. Email content is treated as untrusted data (prompt injection risk).

> Living document: diagrams will be added during weeks 2-4.
