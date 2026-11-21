# Day 13 — Tasks and audit

- Tasks support due dates, priority, status, origin and a polymorphic relation to another entity.
- CRUD is available at `/api/tasks` and the UI at `/tareas`.
- Every task create, update and delete writes an `audit_events` record.
- Audit records can be queried at `/api/audit-events`.
- Migration V11 creates `tasks` and `audit_events`.
- Exactly ten backend tests cover task defaults, relations, CRUD auditing, not-found handling and listing.
