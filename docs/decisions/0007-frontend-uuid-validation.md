# ADR-0007: Frontend UUID validation for synthetic data

- **Status:** Accepted
- **Date:** 2026-11-17
- **Context:** Test and demo data use UUID-shaped identifiers whose version and variant nibbles are `0000`. PostgreSQL and the backend accept these values, but Zod's strict `uuid()` validator rejects them because it requires an RFC-compliant UUID version and variant.

## Decision

Use a shared frontend schema that validates the canonical UUID shape without requiring a specific RFC version or variant:

- Keep backend and database behavior unchanged.
- Use the shared schema for response identifiers across the frontend modules.
- Keep this behavior consistent across development, test, demo, and production builds.
- Treat the synthetic identifiers as test/demo data; production identifiers should continue to be generated as standard UUIDs.

## Consequences

- Test and demo responses with valid UUID-shaped identifiers no longer fail after an HTTP `200` response.
- CRM, clients and services, job opportunities, job applications, proposals, subscriptions, tasks, and authentication responses use the same validation rule.
- The frontend is less strict about UUID RFC version and variant bits. This does not create IDs or bypass backend authorization; it only changes response-shape validation.
- If production data quality must be enforced more strictly in the future, a production-only RFC UUID schema can be introduced without changing the test fixtures.

## Verification

- Frontend lint passes.
- Frontend build passes.
- Vitest passes with 8 test files and 16 tests.
