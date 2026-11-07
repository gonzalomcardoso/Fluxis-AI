# Architecture

Fluxis AI is a modular monolith for the MVP. The browser calls a single Spring
Boot REST API, which owns authentication, authorization, business rules,
persistence, integrations, AI orchestration, approvals, and auditing. PostgreSQL
is the system of record. Microservices and Kubernetes are out of scope for the
MVP.

## Current implementation

```text
Browser (React + TypeScript)
        │  /api REST requests
        │  HTTP session cookie + CSRF token
        ▼
Spring Boot REST API ─────► PostgreSQL 18
```

The current vertical slice supports user registration and login, plus
user-scoped job-opportunity creation and retrieval. The backend uses
Spring Security server-side sessions, an HTTP-only session cookie, CSRF
protection for state-changing requests, and Flyway migrations. OpenAPI
documentation is configured for the REST API.

The implemented backend packages are `auth`, `user`, `job`, and `common`.
Keep capability boundaries explicit as the application grows: a module should
use another module's internal API rather than access its tables directly.
Organize future capabilities such as `email`, `crm`, `services`, `proposals`,
`subscriptions`, `tasks`, `ai`, and `audit` as modules when they are
implemented.

## Target integrations

```text
Spring Boot ─────────► Gmail API
   │                    OAuth 2.0; tokens encrypted in the backend
   └─────────────────► Configurable AI provider (Spring AI)

(Optional, only with a documented benefit)
Spring Boot ── internal authenticated HTTP ──► Python/FastAPI
```

Gmail and AI integrations are planned, not part of the current implementation.
Spring Boot is the default home for AI features. A Python service is optional
and requires a specific, documented benefit; it must not receive Gmail
credentials or authority to execute sensitive actions.

## Responsibilities

- **Frontend:** presentation, forms, UX validation, loading and error states,
  and the approval interface. It does not own business rules or treat an action
  as complete until the backend confirms it.
- **Backend:** authentication, authorization, business rules, persistence,
  integrations, AI orchestration, approvals, auditing, and controlled action
  execution.
- **PostgreSQL:** application data and suggestion/action history.

## Cross-cutting rules

1. AI proposes; the user decides. Never perform a sensitive external action
   without explicit user approval.
2. Approval applies to a specific, idempotent action; the backend validates
   and executes it.
3. Scope every user's data access by `user_id`.
4. Treat email content as untrusted input, including in AI prompts.
5. Keep OAuth tokens in the backend and encrypted at rest; never expose tokens
   or secrets to the browser or store them in `localStorage`.
