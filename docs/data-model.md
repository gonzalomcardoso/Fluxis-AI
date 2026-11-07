# Data Model

Flyway is the source of truth for the database schema. Hibernate validates the
schema at startup; it does not create or update tables. The current schema is
the initial user-authentication and job-opportunity slice. Other product
entities below are planned and have not yet been migrated.

## Implemented schema

### `users`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | Primary key |
| `email` | `VARCHAR(320)` | Required; unique case-insensitively |
| `display_name` | `VARCHAR(200)` | Required |
| `password_hash` | `VARCHAR(100)` | Required; password hash, not plaintext |
| `status` | `VARCHAR(20)` | Required; `ACTIVE` or `DISABLED` |
| `created_at` | `TIMESTAMPTZ` | Required |
| `updated_at` | `TIMESTAMPTZ` | Required |

### `job_opportunities`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | Primary key |
| `user_id` | `UUID` | Required; foreign key to `users.id`, cascade on user deletion |
| `company_name` | `VARCHAR(200)` | Required |
| `title` | `VARCHAR(200)` | Required |
| `url` | `VARCHAR(2048)` | Optional |
| `source` | `VARCHAR(100)` | Optional |
| `posted_on` | `DATE` | Optional |
| `description` | `TEXT` | Optional |
| `requirements` | `TEXT` | Optional |
| `technologies` | `TEXT[]` | Required; defaults to an empty array |
| `status` | `VARCHAR(30)` | Required; constrained to the states below; defaults to `PENDING` |
| `created_at` | `TIMESTAMPTZ` | Required |
| `updated_at` | `TIMESTAMPTZ` | Required |

Allowed opportunity statuses: `PENDING`, `INTERESTING`,
`PREPARING_APPLICATION`, `APPLICATION_SENT`, `INTERVIEW`, `OFFER_RECEIVED`,
`REJECTED`, and `DISCARDED`.

The schema also enforces a case-insensitive unique index on user email, indexes
opportunities by `(user_id, created_at DESC)` and `(user_id, status)`, and a
partial unique index on `(user_id, url)` when the URL is not null. The API
queries opportunities by both their ID and the authenticated user's ID.

The schema is created by
[`V1__create_users_and_job_opportunities.sql`](../backend/src/main/resources/db/migration/V1__create_users_and_job_opportunities.sql)
and extended with credential and user-status columns and a status constraint by
[`V2__add_user_credentials_and_status.sql`](../backend/src/main/resources/db/migration/V2__add_user_credentials_and_status.sql).

## Planned entities

These are roadmap items, not tables in the current schema:

`connected_accounts`, `email_records`, `email_labels`,
`email_label_rules`, `email_action_suggestions`, `companies`, `contacts`,
`job_applications`, `clients`, `services`, `proposals`, `proposal_items`,
`subscriptions`, `tasks`, `ai_suggestions`, and `audit_events`.

Define each entity and its migration as its capability is implemented. Keep
module boundaries explicit; a module must not read or write another module's
tables directly.

## Design criteria

- Store money as `NUMERIC` with an explicit currency; never add amounts across
  different currencies.
- Use `TIMESTAMPTZ` for instants.
- Scope every user's data access by `user_id`, and add appropriate foreign
  keys, unique constraints, check constraints, and indexes in the migration
  that creates each table.
- Use Gmail external IDs, not message subjects, as relationship keys.
- Constrain state values in the database and keep them aligned with application
  types.
- Store OAuth tokens encrypted at rest in the backend; do not persist them in
  the browser.
