# Development and Test Environments

Git branches (`dev`, `test`) do not select runtime profiles. Spring uses the `dev` profile by default; automated tests explicitly select `test`.

## Comparison

| Property | Development (`dev`) | Test (`test`) |
|---|---|---|
| Manual Compose database | `fluxis_dev` | `fluxis_test` |
| PostgreSQL | Compose service `postgres`, PostgreSQL 18 | Compose service `postgres_test` or ephemeral Testcontainers PostgreSQL |
| API | `backend-dev` | `backend-test` (Compose `test` profile) |
| Current backend image | `fluxis-ai-backend:feat-d6` | `fluxis-ai-backend:dev` |
| Local frontend | Vite at `http://localhost:5173` → API `8080` | Vite at `http://localhost:5174` → API `8081` |
| Local PostgreSQL port | `5432` | `5433` |
| Compose volume | `postgres_dev_data` | `postgres_test_data` |
| Spring profile | `dev` (default and explicit in Compose) | `dev` in the current manual instance; automated tests use `test` |
| Flyway | Enabled | Enabled; automated tests apply migrations from scratch |
| Hibernate | `validate` | `validate` |
| Synthetic data | Idempotent seeder: two users with two opportunities each | 120 existing synthetic opportunities; the dev backend connected to this database also seeds its two synthetic accounts |
| OpenAPI | `/v3/api-docs` and `/swagger-ui/index.html` | `/v3/api-docs` and `/swagger-ui/index.html` |
| External services | None | None |

The Compose file remains at the repository root (`compose.yaml`) to preserve existing commands. `.env.example` contains placeholder values for manual testing. `postgres-test` reuses `POSTGRES_PASSWORD`, while its database, username, port, and volume remain separate.

## Local Development

From the repository root, copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`:

```powershell
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Compose sets `SPRING_PROFILES_ACTIVE=dev`. PostgreSQL listens only on `127.0.0.1:5432` and uses `fluxis_dev`. To run the backend with Maven instead of Docker, keep PostgreSQL running and execute the following from `backend/`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

The `dev` profile reads the database password from `POSTGRES_PASSWORD`. CORS for `/api/**` allows only the `APP_FRONTEND_URL` origin (default: `http://localhost:5173`). Local synthetic accounts use `DEV_SEED_PASSWORD` and store password hashes with BCrypt. Change the placeholder password in `.env`; it is not logged. Demo data uses reserved `.test` domains.

In another terminal, start the development frontend from `frontend/`:

```powershell
npm run dev
```

Open `http://localhost:5173`. The Vite proxy forwards `/api` requests to the dev backend on port `8080`.

## Automated Tests

From `backend/`, with Docker running:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd verify
```

Integration tests use Testcontainers and an ephemeral PostgreSQL 18 instance; they do not connect to `fluxis_dev` or the optional Compose service. Flyway applies migrations from scratch in that container.

## Separate Applications for Manual Testing

From the repository root, start the test backend and PostgreSQL, isolated from the development environment:

```powershell
docker compose --profile test up -d --build
docker compose --profile test ps
Invoke-RestMethod http://localhost:8081/actuator/health
```

This starts `postgres-test` on port `5433` and `backend-test` on port `8081`, connected to `fluxis_test` over an internal Compose network. The database has its own username and volume, and reuses the local development `POSTGRES_PASSWORD`; credentials remain in `.env`, which must not be shared. The `test-data` profile loads 120 synthetic opportunities in English for the test user. Its seeder is idempotent and is not enabled in automated Testcontainers tests.

To compare the working branch with `dev`, the JARs are stored locally as `backend/fluxis-ai-backend-feat-d6.jar` and `backend/fluxis-ai-backend-dev.jar` (ignored by Git). Their Docker images use the corresponding tags: `fluxis-ai-backend:feat-d6` and `fluxis-ai-backend:dev`. In this setup, the dev Docker environment runs `feat/d6`, while the test Docker environment runs the `dev` JAR; each has an isolated database. The test backend uses the Spring `dev` profile, with its connection properties explicitly pointing to `postgres-test`. Its seeder enables the synthetic accounts `dev@fluxis.local` and `demo@fluxis.local` with `DEV_SEED_PASSWORD`. The 120 historical opportunities for `integration@fluxis.local` remain associated with an account disabled by the migration and are not visible in those sessions. Routes under `/api/**` require authentication; request `GET /api/auth/csrf` and send the returned token in the specified header with every state-changing request. To rebuild an image tag, check out the intended branch and build it explicitly so Compose does not mix branches.

From `backend/`, build the dev artifact:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-dev.jar
docker build --tag fluxis-ai-backend:dev .
```

On branch `feat/d6`, use the names `fluxis-ai-backend-feat-d6.jar` and `fluxis-ai-backend:feat-d6`. After rebuilding an image tag, recreate its assigned service from the repository root:

```powershell
docker compose up -d backend
docker compose --profile test up -d backend_test
```

### Updating Test After Merging a Feature into Dev

After a `feat/*` PR has been merged into `dev`, the user switches to the `dev` branch. From `backend/`, build the JAR and `dev` image using the integrated code:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-dev.jar -Force
docker build --tag fluxis-ai-backend:dev .
```

From the repository root, update only the test backend and verify its health:

```powershell
docker compose --profile test up -d backend_test
Invoke-RestMethod http://localhost:8081/actuator/health
```

This preserves `postgres-test` and its data. Opening a PR is not enough: `dev` must contain the merge for the artifact to include the feature. The user runs all Git commands and switches branches.

In another terminal, start the second frontend from `frontend/`:

```powershell
npm run dev:test
```

Open `http://localhost:5174`. The Vite `test` mode forwards `/api` to the test backend on port `8081`; the dev frontend on port `5173` continues to use port `8080`. Both applications can be open at the same time without sharing a database.

Stop only the test Docker services without deleting their data:

```powershell
docker compose --profile test stop backend_test postgres_test
```

To recreate only the manual test database, stop it first and delete **only** the `postgres_test_data` volume. The full name usually includes the Compose project prefix; inspect it before removing it:

```powershell
docker volume ls --filter label=com.docker.compose.volume=postgres_test_data
docker volume rm <exact-test-volume-name>
docker compose --profile test up -d postgres_test
```

Do not use `docker compose down -v` to reset only test; it could also delete the development volume. To reset dev, identify and remove only the `postgres_dev_data` volume in the same way; this deletes local development data.

## Verify the Databases Are Separate

```powershell
docker compose ps
docker compose --profile test ps
docker compose exec postgres psql -U fluxis_dev -d fluxis_dev -c "SELECT current_database();"
docker compose --profile test exec postgres_test psql -U fluxis_test -d fluxis_test -c "SELECT current_database();"
```

The services, ports, and volumes are independent. Data is not copied between the environments.

## Legacy `dev` Checkout: `env-dev` and `env-test`

A separate checkout on the `dev` branch can run against the existing development and test databases without replacing the main checkout's containers.

| Environment | Backend image/container | Frontend | API | Database |
|---|---|---|---|---|
| `env-dev` | `fluxis-ai-backend:env-dev` / `env-dev` | `http://localhost:5173` | `http://localhost:8083` | `fluxis_dev`, via `postgres-dev:5432` (host port `5432`) |
| `env-test` | `fluxis-ai-backend:env-test` / `env-test` | `http://localhost:5174` | `http://localhost:8082` | `fluxis_test`, via `postgres-test:5432` (host port `5433`) |

Both backend containers join the existing `fluxis-ai_default` Docker network. Container-to-container JDBC uses PostgreSQL's internal port `5432`; `5433` is the host port published for `postgres-test`. The frontend Vite proxy uses the matching API port for each mode.

Build each image from this checkout's backend source, from `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-env-dev.jar -Force
docker build --file .\Dockerfile.env-dev --tag fluxis-ai-backend:env-dev .

Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-env-test.jar -Force
docker build --file .\Dockerfile.jar --tag fluxis-ai-backend:env-test .
```

From the checkout root, provide the path to the main repository's `.env` file for database credentials and start either or both backends:

```powershell
$envFile = Read-Host "Path to the main checkout .env file"
docker compose --project-name fluxis-env-dev --env-file $envFile --file .\compose.env-dev.yaml up -d
docker compose --project-name fluxis-env-test --env-file $envFile --file .\compose.env-test.yaml up -d
```

`$envFile` and `$composeFile` are PowerShell variables, not entries for `.env`. Keep `.env` in Compose `KEY=value` format; do not put PowerShell assignments in it. To reuse local paths in new PowerShell sessions, add assignments to your PowerShell profile (open it with `notepad $PROFILE`) and replace these example paths with the paths on your machine:

```powershell
$envFile = "C:\path\to\main-checkout\.env"
$composeFile = "C:\path\to\prod-checkout\compose.yaml"
```

Keep the profile on your machine, outside the repository. When both paths are absolute, Compose can be invoked from any working directory by passing `--env-file $envFile` and `--file $composeFile`. Commands that use relative paths without these options still need to run from the documented checkout or application directory.

Start the matching frontend from `frontend/`:

```powershell
npm run dev
npm run dev:test
```

The dev and test API images remain separate (`env-dev` and `env-test`), while both use their corresponding existing database. Do not run `docker compose down -v`; it can remove volumes belonging to the other environments.

## Local Production-Like Environment: `env-prod`

The `prod` Compose profile runs a separate backend container named `env-prod` and PostgreSQL 18 database `fluxis_prod`. It has its own `postgres_prod_data` volume and credentials; it does not share dev/test data. PostgreSQL has no published host port, and the API binds to `127.0.0.1:8084`. The frontend runs at `http://localhost:5175` or `http://127.0.0.1:5175` and proxies API requests to port `8084`; the prod API explicitly allows both local origins.

The `prod` Spring profile does not activate either synthetic-data seeder and disables public registration. Flyway creates the schema only; no users or job opportunities are inserted. Job opportunities should be created through the authenticated application, not by adding seed or sample rows to this database.

This is a local simulation, not a production deployment. Keep its credentials separate from dev/test. The local Compose default disables the `Secure` cookie flag because the frontend uses HTTP; real deployments must use HTTPS and set `SESSION_COOKIE_SECURE=true`. The application currently has no role model, so the one bootstrapped account is the sole application account, not an account with an enforced `admin` role.

Copy `.env.example` to `.env`, replace `POSTGRES_PROD_PASSWORD` with a unique local password, and run these commands from the repository root. They use the `.env` in the current checkout:

```powershell
$envFile = Join-Path (Get-Location) ".env"
docker compose --env-file $envFile --profile prod build backend_prod
docker compose --env-file $envFile --profile prod up -d postgres_prod
docker compose --env-file $envFile --profile prod run --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-bootstrap --spring.main.web-application-type=none
docker compose --env-file $envFile --profile prod up -d backend_prod
docker compose --env-file $envFile --profile prod ps
Invoke-RestMethod http://localhost:8084/actuator/health
```

The bootstrap command prompts for the initial account email, display name, and password without echoing the password. It runs without starting the web server, stores a BCrypt hash, and only runs when the users table is empty; it fails if any account already exists. Public registration remains disabled afterward. Do not run `docker compose down -v`; that removes the environment's persistent data.

If the single account password is forgotten, rebuild the backend image and run this one-time recovery command from the repository root. It targets the existing `fluxis_prod` database, requires exactly one account, verifies its email and asks you to type `RESET`, then prompts for a new password without echoing it. It changes only that account's BCrypt password hash:

```powershell
docker compose --env-file $envFile --profile prod build backend_prod
docker compose --env-file $envFile --profile prod run --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-reset --spring.main.web-application-type=none
```

Do not run the initial `prod-bootstrap` command again; the account already exists.

To verify a password against the existing account without changing the database, rebuild the backend image and run the read-only password check. Enter the account email and password in the interactive prompts. The command reports only the received character counts and whether the password matches; it never prints the password or hash:

```powershell
docker compose --env-file $envFile --profile prod build backend_prod
docker compose --env-file $envFile --profile prod run --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-check --spring.main.web-application-type=none
```

The check requires exactly one account and does not update it. `Length received` counts Unicode code points; letters and digits are classified with Java's Unicode character rules. `Console.readPassword()` reads a line without its line terminator.

Start the matching frontend from `frontend/`:

```powershell
npm run dev:prod
```
