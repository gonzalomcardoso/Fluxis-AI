# Data Model

Flyway is the source of truth for the database schema. Hibernate validates the
schema at startup; it does not create or update tables. Migrations `V1` through
`V14` define the current schema. `V12`-`V14` align proposal, proposal-item,
proposal-history and service monetary/quantity columns to `NUMERIC(19,2)`.

All user-owned tables include a `user_id` foreign key to `users.id` with
`ON DELETE CASCADE`. Application queries must always include the authenticated
user's ID, even when a row UUID is supplied by the client.

## Core identity

### `users`

| Column | Type | Meaning |
|---|---|---|
| `id` | `UUID` | Primary key |
| `email` | `VARCHAR(320)` | Required; unique case-insensitively |
| `display_name` | `VARCHAR(200)` | Required |
| `password_hash` | `VARCHAR(100)` | BCrypt hash |
| `status` | `VARCHAR(20)` | `ACTIVE` or `DISABLED` |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | UTC instants |

## Job search

### `job_opportunities`

Stores saved opportunities. `technologies` is a PostgreSQL `TEXT[]`; status is
constrained to `PENDING`, `INTERESTING`, `PREPARING_APPLICATION`,
`APPLICATION_SENT`, `INTERVIEW`, `OFFER_RECEIVED`, `REJECTED`, or `DISCARDED`.
The pair `(user_id, url)` is unique when the URL is present.

### `job_applications`

One application per `(user_id, job_opportunity_id)`. The application records its
status, applied date, and notes. Deleting an opportunity or user cascades.

### `application_interviews`

Belongs to a job application. Stores `scheduled_at`, interview type, and notes.

### `application_follow_ups`

Belongs to a job application. Stores `due_on`, description, and completion state.

## CRM and commercial data

### `companies`

User-owned companies with a case-insensitive unique name per user.

### `contacts`

User-owned contacts, optionally linked to a company. Email is unique per user
case-insensitively when present.

### `clients`

User-owned clients, optionally linked to a company and contact. Status is
`POTENCIAL`, `ACTIVE`, `INACTIVE`, or `LOST`.

### `services`

User-owned professional services. Stores pricing model (`HOURLY`, `FIXED`, or
`RECURRING`), rate as `NUMERIC(19,2)`, ISO currency, availability, and status.
Names are unique per user case-insensitively.

### `proposals`

A proposal belongs to a user and a client. It stores an ISO currency, status,
valid-until date, and persisted subtotal and total. Status is `DRAFT`,
`PENDING_REVIEW`, `SENT`, `ACCEPTED`, `REJECTED`, or `EXPIRED`.

Proposal status changes are not yet constrained by transition rules. Proposal
subtotals and totals are persisted with NUMERIC(19,2). Line amounts are
rounded to two decimal places before persistence. All monetary values use two
decimal places. Every referenced service must use the same
currency as the proposal.

### `proposal_items`

Belongs to a proposal and references a service. Quantity is stored as
`NUMERIC(19,2)`, unit price as `NUMERIC(19,2)`, and the calculated line
amount as `NUMERIC(19,2)`. Deleting a proposal cascades to its
items.

### `proposal_history`

Stores a snapshot of proposal status, subtotal, and total for each create/update
operation.

### `subscriptions`

User-owned recurring expenses. Amount uses `NUMERIC(19,2)` with an ISO currency.
Periodicity is `WEEKLY`, `MONTHLY`, `QUARTERLY`, or `YEARLY`; status is `ACTIVE`,
`PAUSED`, `CANCELLED`, or `EXPIRED`. Summaries are grouped by currency and must
never add amounts across currencies. Upcoming-renewal views include only dates
between today (UTC) and 30 days ahead.

## Work management

### `tasks`

User-owned tasks with title, description, due date, priority, status, origin, and
optional generic related-entity metadata. Status is `TODO`, `IN_PROGRESS`, `DONE`,
or `CANCELLED`; priority is `LOW`, `MEDIUM`, `HIGH`, or `URGENT`.

### `audit_events`

Append-only, user-scoped events with event type, entity type, entity ID, action,
and JSONB details. Task create, update, and delete operations write audit events
in the same transaction.

## Planned entities

Gmail and AI tables are not part of the current schema. Planned tables include
`connected_accounts`, `email_records`, `email_labels`, `email_label_rules`,
`email_action_suggestions`, and `ai_suggestions`. Define these in new Flyway
migrations when their capabilities are implemented.

## Design rules

- Store money as `NUMERIC` with an explicit ISO currency; never add amounts
  across different currencies.
- Use `TIMESTAMPTZ` for instants and calculate date windows in UTC.
- Scope every user-owned query by `user_id`.
- Add foreign keys, unique constraints, check constraints, and indexes in the
  same migration that creates a table.
- Use Gmail external IDs, not subjects, as integration keys.
- Keep OAuth tokens encrypted in the backend and never persist them in the
  browser.
- Store OAuth ciphertext only; supply encryption keys from outside the database
  and rotate them through an approved procedure.
