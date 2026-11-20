# Environment Validation Workflow

Environment validation is a mandatory, sequential gate and one of the
[Golden Rules](master-prompt.md). A change is not ready for the next environment
until the user has run and approved its checks in the current environment.

## Mandatory order

```text
DEV validated -> TEST validated -> PROD validated
```

Do not skip environments. Do not continue automatically after a successful
environment. Each transition requires explicit user confirmation.

## Gate rules

1. Work only on the current environment.
2. Provide the exact commands and expected results for that environment.
3. The user runs the verification and decides whether the gate passes.
4. Wait for explicit confirmation before preparing the next environment.
5. If a check fails, fix the issue and repeat the current environment's checks.
6. Do not carry an unverified change into the next environment.
7. Record the evidence for every gate.

Automated tests that use Testcontainers are executed during the environment
phase even though they use an ephemeral PostgreSQL database. They do not replace
the manual smoke test against the environment's Compose database.

## DEV gate

DEV is the first validation environment.

Required checks:

- Build the current backend image.
- Start `compose.dev.yaml` and confirm the backend is healthy on port `8083`.
- Confirm the expected Flyway migrations are applied.
- Confirm schema changes, constraints, and numeric precision where applicable.
- Run backend tests, including integration tests when the change affects
  persistence.
- Run frontend lint, type-check, tests, and production build.
- Exercise the changed feature manually through the DEV frontend on port `5173`.
- Confirm no unexpected data or configuration changes remain.

The assistant must stop after presenting the DEV evidence and wait for user
approval.

## TEST gate

TEST starts only after DEV approval.

Required checks:

- Build the current backend image with the `env-test` tag.
- Start `compose.test.yaml` and confirm the backend is healthy on port `8084`.
- Confirm the TEST profiles and isolated test database are in use.
- Confirm the expected Flyway migrations are applied.
- Run backend tests, including PostgreSQL/Testcontainers integration tests.
- Run frontend lint, type-check, tests, and production build.
- Exercise the changed feature manually through the TEST frontend on port `5174`.
- Confirm synthetic test data is the only data affected.

The assistant must stop after presenting the TEST evidence and wait for user
approval.

## PROD gate

PROD starts only after TEST approval.

Required checks:

- Build and start the local PROD simulation and confirm health on port `8085`.
- Confirm Flyway migrations and schema state.
- Confirm public registration and synthetic seeders are disabled.
- Verify CSRF protection is bound to the server-side session.
- Verify session-cookie security settings for the deployment context.
- Verify one-off bootstrap/reset/check commands terminate correctly.
- Run the documented backend and frontend checks.
- Perform a minimal manual smoke test with the production account.
- Confirm no test data, secrets, or development configuration is present.

The assistant must stop after presenting the PROD evidence and wait for user
approval before declaring the change complete.

## Database migration approval

A migration is never applied automatically as part of validation. Before asking
for approval, prepare:

- The Flyway migration file and its version.
- Documentation of the purpose and affected tables.
- Validation queries and expected results.
- Data/backfill impact and failure handling.
- Rollback or forward-fix strategy.

Do not run the migration, run Flyway manually, apply DDL, or restart/recreate a
service whose startup would apply a pending migration until the user explicitly
approves it for the current environment.

## Evidence record

Keep a short record for each gate. A commit hash is optional until the work is
committed.

| Environment | Date | Commit | Health | Backend tests | Frontend tests | Manual check | User approval |
|---|---|---|---|---|---|---|---|
| DEV |  |  |  |  |  |  |  |
| TEST |  |  |  |  |  |  |  |
| PROD |  |  |  |  |  |  |  |

## Failure handling

- A failed check keeps the workflow in the current environment.
- Fix the issue before asking for approval again.
- Re-run the affected checks and the environment smoke test.
- Never use a database volume reset to hide a migration or credential failure.