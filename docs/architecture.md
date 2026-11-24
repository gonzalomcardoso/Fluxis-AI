# Architecture

Fluxis AI is a modular monolith for the MVP. The browser calls a single Spring
Boot REST API, which owns authentication, authorization, business rules,
persistence, integrations, AI orchestration, approvals, and auditing. PostgreSQL
is the system of record. Microservices and Kubernetes are out of scope.

## Current implementation

```text
Browser (React + TypeScript)
        |
        |  /api REST requests
        |  HTTP session cookie + CSRF token
        v
Spring Boot REST API -------> PostgreSQL 18
```

The implemented backend packages are:

- `auth` and `user`: registration, database-backed login, sessions, CSRF, and
  the one-time production account lifecycle.
- `job`: job opportunities.
- `application`: job applications, interviews, and follow-ups.
- `crm`: companies and contacts.
- `client` and `catalog`: clients and professional services.
- `proposal`: proposals, line items, totals, and change history.
- `subscription`: recurring subscriptions and per-currency summaries.
- `task`: tasks and related-entity metadata.
- `audit`: user-scoped audit events.
- `dashboard`: aggregated metrics for the dashboard.
- `common`: shared security, error handling, pagination, CORS, and OpenAPI
  configuration.

The API uses Spring Security server-side sessions, an HTTP-only session cookie,
CSRF protection for state-changing requests, and Flyway migrations. User-owned
reads and writes are scoped by `user_id`; requesting another user's resource
returns `404`.

## Module boundaries

Each business capability is a package/module. Modules may call another module's
public service when they need a business operation. Avoid reaching into another
module's repository or table directly. The dashboard is an aggregation layer and
is expected to use published module queries rather than owning data.

Future capabilities such as `email` and `ai` should be added as new modules.
Use explicit APIs between modules; do not share JPA entities as public contracts.

## Target integrations

```text
Spring Boot --------> Gmail API
   |                    OAuth 2.0; tokens encrypted in the backend
   |
   +----------------> Configurable AI provider (Spring AI)
```

Gmail connection storage and token encryption are implemented; the interactive
OAuth flow, synchronization, and Gmail actions are planned for days 16-21. AI
integration is planned for days 22-27. Spring Boot remains the default home for
AI features. A Python service is optional and requires a specific, documented
benefit; it must not receive Gmail credentials or authority to execute sensitive
actions.

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
6. Never mix currencies in a proposal or dashboard total.
7. State-changing requests require both an authenticated session and a CSRF
   token bound to that session.
