# Fluxis AI Backend

## Requirements

- JDK 25.
- Docker for integration tests with Testcontainers.
- Local PostgreSQL only when running the application outside Docker.

The backend uses the included Maven Wrapper; a global Maven installation is not required.

## Tests

From `backend/`, in PowerShell:

```powershell
.\mvnw.cmd test
```

Runs service unit tests and `@WebMvcTest` web tests without starting PostgreSQL.

```powershell
.\mvnw.cmd verify
```

Also runs `*IT` integration tests through Maven Failsafe. These use a PostgreSQL 18 container shared during the test run; they do not use H2 or require an additional PostgreSQL service. The first run downloads the required images and dependencies.

To explicitly run tests with the `test` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd verify
```

## Test Naming Convention

- `*Test`: unit or web-layer tests; Maven Surefire runs them during `test`.
- `*IT`: integration tests with real PostgreSQL; Maven Failsafe runs them during `verify`.

Flyway migrations are applied when each integration-test context starts against the test container.
