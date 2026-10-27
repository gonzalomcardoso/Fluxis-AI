# Data Model

> Pending. The model will be defined through Flyway migrations starting on day 3 and completed by day 7.

Tablas previstas: `users`, `connected_accounts`, `email_records`, `email_labels`, `email_label_rules`, `email_action_suggestions`, `companies`, `contacts`, `job_opportunities`, `job_applications`, `clients`, `services`, `proposals`, `proposal_items`, `subscriptions`, `tasks`, `ai_suggestions`, `audit_events`.

## Design criteria

- Store monetary amounts as `NUMERIC` (never `float`), with an explicit currency; do not add amounts in different currencies.
- Use time-zone-aware timestamps (`timestamptz`) when representing instants.
- Use Gmail external IDs as relationship keys (never the subject line).
- Constrain states (enums with constraints).
- Add foreign keys, unique constraints, and indexes in the first migration.
