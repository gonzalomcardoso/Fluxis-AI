# Security

## Authentication

The API uses database-backed Spring Security authentication with BCrypt password hashes and server-side sessions. The session cookie is `HttpOnly` and `SameSite=Lax`; credentials and session identifiers are not stored in `localStorage`. Local HTTP development leaves the cookie `Secure` flag disabled; a real production deployment must use HTTPS and set `SESSION_COOKIE_SECURE=true`.

In dev/test, create an account with `POST /api/auth/register`, then authenticate with `POST /api/auth/login`. Registration is denied in the `prod` profile; the initial account is created through the one-time `prod-bootstrap` command documented in [environments.md](./environments.md). `GET /api/auth/me` returns the current account and `POST /api/auth/logout` ends the session. Obtain a CSRF token from `GET /api/auth/csrf` and send its value in the returned header for every state-changing request, including registration in dev/test, login, and logout. CORS allows only the configured frontend origin and credentials.

`/actuator/health`, the CSRF bootstrap endpoint, and login are public. Registration is public only when `app.auth.registration-enabled=true` (the default for dev/test); the prod profile sets it to false. Other `/api/**` endpoints require an authenticated session and return `401` when unauthenticated. Job-opportunity reads and writes are scoped to the authenticated user; requesting another user's opportunity returns `404`, and list queries never include another user's records. This ownership boundary is covered by PostgreSQL integration tests. Apply the same rule to every new user-owned resource and operation.

The OpenAPI specification is available at `/v3/api-docs`, with Swagger UI at `/swagger-ui/index.html`. Its description documents session-cookie and CSRF requirements.

The frontend checks the current session on load, sends cookies and CSRF headers from a shared API client, and signs out through the backend. Its prod mode hides public registration. The session identifier and CSRF token are not persisted in browser storage.

## Existing accounts and local seed data

Migration V2 disables all pre-existing accounts and assigns a placeholder BCrypt hash because no original passwords exist to migrate safely. The dev seeder activates only its synthetic local accounts using `DEV_SEED_PASSWORD`. The optional test-data seeder uses `TEST_SEED_PASSWORD`. Both example values are for local use only and should be replaced in `.env`; never log or commit actual credentials.

## Decisions and remaining controls

The session-cookie decision and alternatives are recorded in [ADR-0006](./decisions/0006-session-cookie-authentication.md). The current user model has no roles; the single prod-bootstrap account is the sole account, not a role-based admin. Password reset, rate limiting, production session-store setup, and ownership checks for future resource modules remain future work.
