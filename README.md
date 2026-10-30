# Fluxis AI

Professional productivity web platform: intelligent email management (Gmail), job search, lightweight CRM, services and proposals, subscriptions, and tasks, with AI that **proposes** and a user who **decides**.

> **Core principle: AI proposes; the user decides.**
> AI never sends emails, moves messages, or deletes information without explicit approval. Every approved action is validated and executed by the backend, and the interface marks it as complete only after server confirmation.

**Status:** in development (30-day plan). Initially for personal use; the architecture is designed to evolve to support multiple users.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript (strict), Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, Vitest, React Testing Library |
| Backend | Java 25 LTS, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway, Spring AI, JUnit, Mockito, Testcontainers |
| Data | PostgreSQL 18 |
| Infrastructure | Docker, Docker Compose, GitHub Actions |
| Optional | Python (FastAPI) only if there is a specific benefit; see [ADR-0002](docs/decisions/0002-modular-monolith-and-python.md) |

Exact versions and rationale: [ADR-0001](docs/decisions/0001-stack-versions.md).

## Repository structure

```
fluxis-ai/
├── backend/            # Spring Boot (modular monolith)
├── compose.yaml        # PostgreSQL 18 and backend services
├── frontend/           # React + TypeScript + Vite
├── docs/
│   ├── architecture.md
│   ├── data-model.md
│   ├── roadmap.md      # 30-day plan
│   └── decisions/      # ADRs: technical decision records
├── .github/workflows/  # CI (day 5)
├── .env.example        # environment variable template
└── README.md
```

## Getting started

- [ ] Day 4: start the frontend (`npm run dev`).
- [ ] Day 5: run tests and CI.

Prerequisites: Docker with Compose. Install Node.js 24 LTS (`.nvmrc`) to run the frontend, or JDK 25 (Temurin) to run the backend outside Docker.

```bash
cp .env.example .env   # Fill in the values; never commit .env to Git
docker compose up --build -d
```

Compose starts PostgreSQL 18 and the Spring Boot backend. The backend waits for PostgreSQL's health check before starting, and its own health check calls `/actuator/health`:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

On Windows PowerShell, check the endpoint with:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

To run the backend locally instead, start PostgreSQL with `docker compose up -d postgres`, then from `backend/` run:

```powershell
.\mvnw.cmd spring-boot:run
```

The local database defaults match `.env.example`. If you change the database credentials or port there, export the matching `POSTGRES_*` variables in your shell before starting the backend.

## Security

- Secrets and OAuth tokens are **never** stored in the repository or in `localStorage`.
- Google tokens remain in the backend and are stored encrypted.
- Use the minimum required Gmail scopes; document the retention policy before syncing email.

## Git workflow

- `main`: stable. `dev`: daily integration. Work branches: `feat/...`, `fix/...`, `docs/...`.
- Use Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`).
- See [ADR-0003](docs/decisions/0003-git-workflow.md) for details.

## License

[Apache License 2.0](LICENSE)
