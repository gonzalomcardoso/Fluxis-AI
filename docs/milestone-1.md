# Milestone 1: Job Opportunities, Testing, and Continuous Integration

This document explains how to start the application from a clean clone, verify persistence, and run local checks and GitHub Actions.

## Prerequisites

- JDK 25 (Temurin recommended).
- Node.js 24 LTS and npm.
- Docker Desktop or Docker Engine with Compose and an active daemon.
- Git.

The backend uses the included Maven Wrapper; a global Maven installation is not required.

## Start from a Clean Clone

### 1. Local Configuration and Services

From PowerShell:

```powershell
git clone https://github.com/gonzalomcardoso/Fluxis-AI.git
cd Fluxis-AI
Copy-Item .env.example .env
```

Edit `.env` and replace `POSTGRES_PASSWORD` with a unique local password. Do not share or commit `.env`; Git ignores this file. `VITE_*` variables are public and must not contain secrets.

Start PostgreSQL and the backend:

```powershell
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Wait until both services are healthy before continuing.

### 2. Start the Frontend

In a second terminal:

```powershell
cd C:\path\to\clone\Fluxis-AI\frontend
npm ci
npm run dev
```

Open the URL printed by Vite, usually `http://localhost:5173`. The development proxy forwards `/api` to the backend at `localhost:8080`.

### 3. Create and Verify an Opportunity

Sign in with the local synthetic account `dev@fluxis.local` and the password configured by `DEV_SEED_PASSWORD`. Open the opportunity creation form, enter a company and job title, and save. The opportunity should appear in the list. Reload the page to confirm it is retrieved from the backend.

The API can also be exercised from PowerShell. The example uses the dev seed account; set `DEV_SEED_PASSWORD` in the shell to the same value configured in `.env`:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod `
  -Uri http://localhost:8080/api/auth/csrf `
  -WebSession $session
$headers = @{}
$headers[$csrf.headerName] = $csrf.token

$login = @{
  email = "dev@fluxis.local"
  password = $env:DEV_SEED_PASSWORD
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/auth/login `
  -WebSession $session `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $login

$body = @{
  companyName = "Sample Company"
  title = "Platform Developer"
  url = "https://jobs.example.test/fluxis-milestone-1"
  source = "Local test"
  technologies = @("Java", "PostgreSQL")
  status = "PENDING"
} | ConvertTo-Json

$created = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/job-opportunities `
  -WebSession $session `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body

$created
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/job-opportunities/$($created.id)" `
  -WebSession $session
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/job-opportunities?page=0&size=10" `
  -WebSession $session
```

To verify the persisted row directly in PostgreSQL (using the default values from `.env.example`):

```powershell
docker compose exec postgres psql -U fluxis_dev -d fluxis_dev `
  -c "SELECT id, company_name, title, status FROM job_opportunities ORDER BY created_at DESC LIMIT 10;"
```

If you changed `POSTGRES_DEV_USER` or `POSTGRES_DEV_DB`, use those values in the command. The examples create synthetic data; remove the test opportunity from the local database if it is no longer needed. See [Development and Test Environments](./environments.md) for the manual test service and reset procedures that do not affect the other database.

## Local Tests

### Backend

From `backend/`:

```powershell
.\mvnw.cmd test
```

Runs unit and web tests without PostgreSQL. To include integration tests with PostgreSQL 18, run this with Docker active:

```powershell
.\mvnw.cmd verify
```

The naming convention is `*Test` for Surefire and `*IT` for Failsafe. See the [backend README](../backend/README.md) for details.

### Frontend

From `frontend/`:

```powershell
npm ci
npm run test:run
npm run lint
npm exec -- tsc --noEmit
npm run build
```

Component tests use Vitest and React Testing Library in `jsdom`. API access is mocked at the API-module boundary with Vitest; MSW is not used because these tests focus on component/hook behavior and the HTTP layer is already centralized.

## GitHub Actions

The [CI workflow](../.github/workflows/ci.yml) runs on pushes and pull requests targeting `dev`, `test`, `prod`, and `main`. No GitHub Environment, secrets, or PostgreSQL service configuration is required: the Ubuntu runner provides Docker, and Testcontainers starts PostgreSQL 18 for backend tests.

Backend and frontend jobs run in parallel. The backend uses Java 25 and runs `./mvnw verify`; the frontend uses the version in `.nvmrc` and runs reproducible installation, lint, type checking, non-interactive tests, and a build. If a test fails, Surefire/Failsafe or JUnit reports are uploaded as artifacts to help diagnose the run.

To see the result on GitHub, push the branch:

```powershell
git push origin test
```

If the remote `test` branch does not exist yet, create it from the commit you want to verify and publish it with `git switch -c test` and `git push -u origin test`. The workflow also runs on `dev`, `prod`, and `main`. The `dev` status appears in the CI badge in the [root README](../README.md).

## Milestone Checklist

- [ ] PostgreSQL and the backend start healthy from Compose.
- [ ] An opportunity can be created through the interface.
- [ ] The opportunity remains after a reload and can be retrieved through the API.
- [ ] The row appears in PostgreSQL.
- [ ] Backend: `.\mvnw.cmd verify` passes with Testcontainers/PostgreSQL 18.
- [ ] Frontend: tests, lint, type checking, and build pass.
- [ ] The CI workflow passes on GitHub for the published branch.

## Known Limitations

- Gmail, AI, and editing or deleting opportunities are not implemented.
- Backend integration tests require Docker; unit and web tests do not.
- CI tests changes; it does not deploy the application.
