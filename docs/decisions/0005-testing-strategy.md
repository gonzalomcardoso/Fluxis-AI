# ADR-0005: Testing Strategy

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

The first end-to-end feature persists PostgreSQL-specific data, including `text[]`, and the frontend API is already centralized behind one module. The milestone needs repeatable backend and UI evidence without production credentials.

## Decision

- Use JUnit 5 and Mockito for service unit tests, `@WebMvcTest` for the HTTP contract, and Testcontainers with PostgreSQL 18 for repository and API integration tests. Run `*IT` with Maven Failsafe during `verify`.
- Reuse a single PostgreSQL container during an integration-test JVM. Do not use H2: it would not validate Flyway or PostgreSQL array mappings against the production database engine.
- Mock the frontend API module with Vitest in component tests instead of adding MSW. This keeps tests focused on accessible UI behavior and uses the existing API boundary without an additional dependency.
- Run backend and frontend verification as independent GitHub Actions jobs on pushes and pull requests to `dev`, `test`, `prod`, and `main`. CI uses no application secrets or separately managed GitHub Environment.

## Consequences

- Backend integration tests need Docker locally; GitHub-hosted Ubuntu runners provide it.
- The frontend tests do not exercise browser networking or the live backend; the documented manual milestone flow verifies UI-to-PostgreSQL persistence.
- Failsafe reports are distinct from Surefire reports, making unit/web failures and database integration failures identifiable.
