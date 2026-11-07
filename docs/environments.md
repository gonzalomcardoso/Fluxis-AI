# Local Environments

Each local environment has its own Compose file, variable file, project, network, database credential, and persistent database volume.

| Environment | Compose file | Local env file | Backend URLs | PostgreSQL volume |
|---|---|---|---|---|
| DEV | `compose.dev.yaml` | `.env.dev` | `8083` | `fluxis-ai_postgres_dev_data` |
| TEST | `compose.test.yaml` | `.env.test` | `8082` | `fluxis-ai_postgres_test_data` |
| PROD | `compose.prod.yaml` | `.env.prod` | `8084` | `fluxis-ai_postgres_prod_data` |

The Compose files are self-contained and use separate Docker networks. They retain the existing named database volumes so recreating a container does not remove its data. Do not use `docker compose down -v` or `docker volume rm` when switching configurations.

## One-Time Local Setup

Run these commands from the configuration checkout root, or replace `.` with the absolute path to that root:

```powershell
Copy-Item .env.dev.example .env.dev
Copy-Item .env.test.example .env.test
Copy-Item .env.prod.example .env.prod
```

Edit each new env file locally and replace the placeholder values. Never commit the `.env.dev`, `.env.test`, or `.env.prod` files. They are ignored by Git; the `.env.*.example` templates are safe to commit.

For databases that already contain data, each `POSTGRES_*_PASSWORD` value must match the password currently stored for that database role. Changing an env file does not rotate a PostgreSQL role password. If rotating a role, use an interactive `psql` `\password <role>` command before recreating its backend. Use separate values for DEV, TEST, and PROD when rotating credentials.

The PowerShell profile can point to the configuration root so Compose works from any current directory:

```powershell
$composeRoot = $prodRoot
$devEnvFile = Join-Path $composeRoot ".env.dev"
$testEnvFile = Join-Path $composeRoot ".env.test"
$prodEnvFile = Join-Path $composeRoot ".env.prod"
$devComposeFile = Join-Path $composeRoot "compose.dev.yaml"
$testComposeFile = Join-Path $composeRoot "compose.test.yaml"
$prodComposeFile = Join-Path $composeRoot "compose.prod.yaml"
```

Build the dev and test backend images from the checkouts containing the code you want to run. The profile's `$devRoot` and `$testRoot` variables point to those checkouts:

```powershell
Push-Location (Join-Path $devRoot "backend")
docker build --file Dockerfile --tag fluxis-ai-backend:env-dev .
Pop-Location

Push-Location (Join-Path $testRoot "backend")
docker build --file Dockerfile --tag fluxis-ai-backend:env-test .
Pop-Location
```

The PROD Compose file builds its backend from the centralized configuration checkout.

## Validate and Start an Environment

`config --quiet` validates interpolation and the Compose model without starting or changing containers:

```powershell
docker compose --env-file $devEnvFile --file $devComposeFile config --quiet
docker compose --env-file $testEnvFile --file $testComposeFile config --quiet
docker compose --env-file $prodEnvFile --file $prodComposeFile config --quiet
```

Start only the environment you want:

```powershell
docker compose --env-file $devEnvFile --file $devComposeFile up -d
docker compose --env-file $testEnvFile --file $testComposeFile up -d
docker compose --env-file $prodEnvFile --file $prodComposeFile up -d --build
```

The dev and test Compose files run the image tags built above and do not build those images themselves.

Check health:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8083/actuator/health
Invoke-RestMethod http://localhost:8084/actuator/health
```

Dev frontend: run `npm run dev` from `frontend/`; it proxies to port `8083`.
Test frontend: run `npm run dev:test` from `frontend/`; it proxies to port `8082`.
PROD frontend: run `npm run dev:prod` from `frontend/`; it proxies to port `8084`.

Automated backend integration tests use Testcontainers and an ephemeral PostgreSQL instance; they do not connect to these manual Compose databases. Run `.\mvnw.cmd verify` from `backend/`.

After starting an environment, wait for its backend health check to pass before testing the frontend.

If a backend fails with `password authentication failed`, stop there: verify that its env file contains the password matching the PostgreSQL role. Do not delete the database volume to fix a credential mismatch.

## Environment Data and Security

DEV and TEST run synthetic local accounts and data; PROD disables public registration and synthetic seeders. The PROD database is separate and should contain only data created through the application. PostgreSQL ports for DEV and TEST bind to loopback; PROD PostgreSQL has no published host port.

The local PROD simulation uses HTTP and defaults `SESSION_COOKIE_SECURE=false`. Real deployments must use HTTPS and set `SESSION_COOKIE_SECURE=true`. The application has no role model; its single bootstrapped account is not an enforced role-based admin.

To bootstrap the initial PROD account, reset its password, or verify a password against the existing hash, use the interactive commands below from the configuration checkout root. They use `.env.prod` and do not expose the password in command history. Use `--project-name fluxis-env-prod` so the one-off command joins the PROD Compose network and can resolve `postgres_prod`. `--build` includes the current backend source, including the one-off command:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-bootstrap --server.port=0
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-reset --server.port=0
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-check --server.port=0
```

Bootstrap is only for an empty users table. Reset and check require exactly one account; password check is read-only. Do not run bootstrap again after the account exists.
