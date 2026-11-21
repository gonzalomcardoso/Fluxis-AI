# Security

## Authentication

The API uses database-backed Spring Security authentication with BCrypt password hashes and server-side sessions. The session cookie is `HttpOnly` and `SameSite=Lax`; credentials and session identifiers are not stored in `localStorage`. Local HTTP development leaves the cookie `Secure` flag disabled; a real production deployment must use HTTPS and set `SESSION_COOKIE_SECURE=true`.

In dev/test, create an account with `POST /api/auth/register`, then authenticate with `POST /api/auth/login`. Registration is denied in the `prod` profile; the initial account is created through the one-time `prod-bootstrap` command documented in [environments.md](./environments.md). `GET /api/auth/me` returns the current account and `POST /api/auth/logout` ends the session. Obtain a CSRF token from `GET /api/auth/csrf` and send its value in the returned header for every state-changing request, including registration in dev/test, login, and logout. CORS allows only the configured frontend origin and credentials.

`/actuator/health`, the CSRF bootstrap endpoint, and login are public. Registration is public only when `app.auth.registration-enabled=true` (the default for dev/test); the prod profile sets it to false. Other `/api/**` endpoints require an authenticated session and return `401` when unauthenticated. Job opportunities, applications, CRM records, clients, services, proposals, subscriptions, tasks, audit events, and dashboard reads are scoped to the authenticated user. Requesting another user's resource returns 404, and list queries never include another user's records. The CSRF token is stored in the server-side session and must be presented with every state-changing request. Integration tests verify that a token from one session cannot be reused in another session. Apply the same ownership and CSRF rules to every new user-owned resource and operation.

The OpenAPI specification is available at `/v3/api-docs`, with Swagger UI at `/swagger-ui/index.html`. Its description documents session-cookie and CSRF requirements.

The frontend checks the current session on load, sends cookies and CSRF headers from a shared API client, and signs out through the backend. Its prod mode hides public registration. The session identifier and CSRF token are not persisted in browser storage.

## Existing accounts and local seed data

Migration V2 disables all pre-existing accounts and assigns a placeholder BCrypt hash because no original passwords exist to migrate safely. The dev seeder activates only its synthetic local accounts using `DEV_SEED_PASSWORD`. The optional test-data seeder uses `TEST_SEED_PASSWORD`. Both example values are for local use only and should be replaced in `.env`; never log or commit actual credentials.

## Decisions and remaining controls

The session-cookie decision and alternatives are recorded in [ADR-0006](./decisions/0006-session-cookie-authentication.md). The current user model has no roles; the single prod-bootstrap account is the sole account, not a role-based admin. Password reset, rate limiting, production session-store setup, and ownership checks for future resource modules remain future work.

## Owner consent for protected changes

> **NINGÚN AGENTE, ASISTENTE O AUTOMATIZACIÓN PUEDE MODIFICAR CONFIGURACIONES, CREDENCIALES O SECRETOS SIN MI CONSENTIMIENTO EXPLÍCITO Y PREVIO.**

The following operations are protected and require explicit prior owner consent:

- changing PostgreSQL passwords;
- running `ALTER ROLE` or modifying database users and permissions;
- modifying `.env`, `.env.dev`, `.env.test`, `.env.prod` or any secrets file;
- changing environment variables or connection credentials;
- changing ports, domains, URLs, certificates or session cookies;
- modifying Docker Compose, volumes, networks or persistence;
- modifying Spring Boot profiles or security/authentication configuration;
- modifying deployment configuration;
- changing user or service permissions.

Before any protected operation, the agent must explain the proposed change, the
reason, affected files/services/data, risks and consequences, present the exact
change, and wait for explicit authorization.

A generic request to fix or diagnose a problem is not authorization. Previous
authorization for another operation is not authorization. Read-only inspection
is allowed; protected changes are not.

## Production protections

- Never run `docker compose down -v` in PROD.
- Never delete or recreate the PostgreSQL volume to troubleshoot the backend.
- Never run migrations without express owner authorization.
- Never modify production data to facilitate tests.
- Never change passwords to troubleshoot without authorization.
- Never rebuild or deploy PROD services without express authorization.
- Never use DEV or TEST credentials in PROD.
- Never expose secrets in logs, documentation, shared terminals or responses.
- Never assume PROD is empty before a destructive operation.

See [production-incidents-and-recovery.md](production-incidents-and-recovery.md)
for the official diagnosis and recovery procedure.

## Secret handling

- `.env`, `.env.dev`, `.env.test` and `.env.prod` are local files ignored by
  Git. They must never be committed.
- Do not print complete container environments; filter to non-secret values.
- Do not include real passwords, tokens or hashes in documentation, issue
  reports or chat messages.
- Use an owner-approved secret manager or local secure storage for real values.
- Do not reuse DEV or TEST credentials in PROD.
- If a secret is exposed, stop, report the exposure without repeating the
  value, and let the owner decide whether rotation is required.
