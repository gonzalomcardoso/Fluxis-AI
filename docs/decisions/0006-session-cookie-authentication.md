# ADR-0006: Session Cookie Authentication

- Status: Accepted
- Date: 2026-11-05

## Context

The React SPA needs authentication without storing credentials or access tokens in browser local storage. The backend must authenticate database-backed users and support an eventual production login flow.

## Decision

Use server-side Spring Security sessions. The browser receives the session identifier only in an `HttpOnly`, `SameSite=Lax` cookie; unsafe API requests must also include a synchronizer CSRF token obtained from `GET /api/auth/csrf`. The CSRF token is held in frontend memory, not local storage. CORS is restricted to the configured frontend origin and allows credentials. Passwords are stored with BCrypt.

Registration is available through `POST /api/auth/register`; login uses `POST /api/auth/login`; the authenticated account is returned by `GET /api/auth/me`; logout uses `POST /api/auth/logout`. `/actuator/health` is public. API routes require authentication.

## Alternatives considered

- HTTP Basic: simpler to bootstrap, but the browser repeatedly sends reusable credentials and it is not the selected SPA session design.
- Bearer tokens in local storage: rejected because an XSS vulnerability could read and exfiltrate tokens.
- OAuth: deferred until provider integrations are explicitly in scope.

## Consequences

- The frontend must send cookies and obtain/send CSRF tokens for state-changing requests.
- Local development uses non-secure cookies over localhost HTTP; deployments must use HTTPS and set `SESSION_COOKIE_SECURE=true`.
- Existing users from before this migration receive disabled accounts and an unusable placeholder BCrypt hash. They cannot log in until credentials are explicitly reset or the development seeder activates synthetic accounts with a local-only password.
- Session storage is currently servlet-container memory; a shared session store is needed before running multiple production backend replicas.
