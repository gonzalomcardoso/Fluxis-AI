# 30-Day Plan — Fluxis AI

Plan based on the six phases of the master prompt. Planned commitment: 5–6 hours per day (approximately 1 hour of learning, 3–4 hours of building, and 1 hour of testing and documentation). Each day ends with a verifiable deliverable. Days 7, 14, and 21 include time to catch up on delays.

## Week 1: Phase 1 — Foundations (days 1–7)

| Day | Tasks | Verifiable deliverable |
|---|---|---|
| 1 | Create `fluxis-ai/`, the folder structure, `README`, `.gitignore`, and `.env.example`. Verify current stable versions and record them in `docs/decisions`. | GitHub repository with ADR-001 (versions). |
| 2 | `docker-compose` with PostgreSQL. Spring Boot skeleton with Web, JPA, Validation, Flyway, and Actuator. | `/actuator/health` is green with the database connected. |
| 3 | V1 migration (`users`, `job_opportunities`); entity, repository, service, and controller; validated DTOs and global error handling. | Opportunity `POST` and `GET` endpoints working. |
| 4 | Frontend with Vite, React, strict TypeScript, Tailwind, Router, TanStack Query, React Hook Form, and Zod. Form and list. | Create an opportunity in the UI and see it in the list. |
| 5 | Tests with JUnit and Testcontainers on the backend, Vitest and RTL on the frontend, and a GitHub Actions pipeline. | **Milestone 1:** documented and verifiable steps. |
| 6 | Basic Spring Security, `user_id` in all queries (user-level data isolation), and OpenAPI. | A user cannot view another user's data, verified by a test. |
| 7 | Catch-up time and review. Update `docs/architecture.md` and `docs/data-model.md`. | Documentation up to date. |

## Week 2: Phase 2 — Data Management (days 8–14)

| Day | Tasks | Verifiable deliverable |
|---|---|---|
| 8 | Companies and contacts: migrations, API, and UI. | Full CRUD with duplicate detection. |
| 9 | Job applications: statuses, interviews, notes, and follow-ups. | Pending → Offer/Rejected workflow. |
| 10 | Clients and professional services (hourly, fixed-price, and recurring). | CRUD with validation. |
| 11 | Proposals and line items. Amounts with `BigDecimal`, statuses, and change history. | Correct calculations with tests. |
| 12 | Subscriptions: monthly and annual spend by currency; upcoming renewals. | Summary that does not mix currencies. |
| 13 | Tasks related to other entities. `audit_events` foundation. | Linked tasks and active auditing. |
| 14 | Dashboard v1 with real data. Catch-up time and testing. | Dashboard with metrics from the data modules. |

## Week 3: Phase 3 — Gmail (days 15–21)

| Day | Tasks | Verifiable deliverable |
|---|---|---|
| 15 | Google Cloud project and consent screen. Minimum scopes. `connected_accounts` with encrypted tokens. Create `docs/gmail-integration.md`. | Permissions, costs, and risks documented. |
| 16 | Backend OAuth flow (state/PKCE), connection status, disconnection, and revocation. | Connect and disconnect a test account. |
| 17 | Paginated metadata sync, no duplicates (Gmail ID), and a retention policy. | `email_records` populated without importing the entire mailbox. |
| 18 | Gmail labels, many-to-many relationship with messages, and visual folders in the UI. | Filter emails by label. |
| 19 | Suggestion model and statuses (`PENDING`…`EXPIRED`), idempotency, and backend execution: apply or remove a label and archive, distinguishing removing, deleting, and permanently deleting. | An approved action is executed only once. |
| 20 | Custom rules (`email_label_rules`): visible, editable, and disableable; they only generate suggestions. | Rules that generate pending suggestions. |
| 21 | Catch-up time. Tests with simulated Gmail, retries, and error handling. | Stable test suite with no real emails or tokens. |

## Week 4 and Wrap-up: Phases 4, 5, and 6 (days 22–30)

| Day | Tasks | Verifiable deliverable |
|---|---|---|
| 22 | Spring AI with a configurable provider. Email summaries, prompt-injection defenses, limits, and cost control. | Summaries that treat email as untrusted data. |
| 23 | Classification with validated structured output: category, label, rationale, and confidence. | AI suggestions persisted. |
| 24 | Approval panel: approve, reject, change destination, review the email, and handle expiration. | Complete approval workflow in the UI. |
| 25 | Extract dates, tasks, opportunities, and leads; deduplicate and require approval before creation. | Tasks and contacts created only after confirmation. |
| 26 | AI-generated Gmail drafts (no sending). Job-offer vs. resume comparison without inventing experience. | Draft created in Gmail for manual review. |
| 27 | Assistant with controlled tools (authorization, limits, and auditing). Distinguish facts, inferences, proposed actions, and executed actions. | Responses based on authorized data. |
| 28 | Security review, E2E tests with synthetic data, and prompt evaluation (Python only if justified). | Security checklist approved. |
| 29 | Dockerfiles, production Compose, CI/CD, deployment decision (AWS or another provider), and basic observability. | Application deployed or ready to deploy. |
| 30 | Portfolio README, demo, final documentation, retrospective, and backlog. | Portfolio-ready project. |

## Rules for Staying on Track

- **Definition of done:** it works, has tests, and is documented. If anything is missing, the day is not complete.
- **If behind schedule,** defer pgvector, the Python service, Google Calendar, and visual polish first. Do not cut security or human approval.
- **Do not start Gmail or AI** until Milestone 1 is complete and the core data is stable (day 14).
