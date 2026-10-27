# ADR-0002: Modular Monolith and Python Policy

- **Status:** Accepted
- **Date:** 2026-10-27

## Decision

1. Initial architecture: a **modular monolith** with Spring Boot as the primary backend (business logic, security, persistence, approvals, and action execution).
2. Implement AI in the backend with **Spring AI** whenever practical.
3. **Do not create a Python service by default.** Add a FastAPI service only when there is a specific benefit (for example, a specialized Python library that would not be practical to use in Java).

## Conditions for adding Python

- Documented API with validated inputs and outputs (Pydantic).
- Communicate with Spring Boot over HTTP, using service authentication.
- Automated tests (Pytest) and linting with Ruff.
- No access to Gmail OAuth credentials.
- No execution of sensitive external actions.
- Do not expose the service publicly without protection.
- Do not duplicate business logic that already exists in Java.

## Consequences

- Less infrastructure and a smaller attack surface in the MVP.
- Python remains available for prompt evaluation and experiments (outside the critical path).
