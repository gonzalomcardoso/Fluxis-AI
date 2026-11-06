# Security

## Authentication

The API uses database-backed Spring Security authentication with BCrypt password hashes and server-side sessions. The session cookie is `HttpOnly` and `SameSite=Lax`; credentials and session identifiers are not stored in `localStorage`. Local HTTP development leaves the cookie `Secure` flag disabled; production must use HTTPS and enable `SESSION_COOKIE_SECURE`.

Register with `POST /api/auth/register`, then authenticate with `POST /api/auth/login`. `GET /api/auth/me` returns the current account and `POST /api/auth/logout` ends the session. Obtain a CSRF token from `GET /api/auth/csrf` and send its value in the returned header for every state-changing request, including registration, login, and logout. CORS allows only the configured frontend origin and credentials.

`/actuator/health` and the CSRF bootstrap, registration, and login endpoints are public. Other `/api/**` endpoints require an authenticated session and return `401` when unauthenticated. Authorization by resource owner, including `404` responses for another user's resources, is the next isolation step and must be applied to every operation before treating the API as complete.

## Existing accounts and local seed data

Migration V2 disables all pre-existing accounts and assigns a placeholder BCrypt hash because no original passwords exist to migrate safely. The dev seeder activates only its synthetic local accounts using `DEV_SEED_PASSWORD`. The optional test-data seeder uses `TEST_SEED_PASSWORD`. Both example values are for local use only and should be replaced in `.env`; never log or commit actual credentials.

## Decisions and remaining controls

The session-cookie decision and alternatives are recorded in [ADR-0006](./decisions/0006-session-cookie-authentication.md). Admin roles, password reset, rate limiting, production HTTPS/session-store setup, and the complete cross-user authorization suite remain future work.
