# Fluxis AI - Master Prompt

This document defines the highest-priority operating rules for work on Fluxis AI.
If any other instruction, plan, or suggestion conflicts with these rules, these
rules take precedence.

## Non-negotiable order of priorities

1. Security and integrity of data, especially PROD.
2. Explicit owner consent for protected changes.
3. Compliance with this master prompt and the Golden Rules.
4. Official project documentation and procedures.
5. Verified evidence from code and real environments.
6. General best practices and technical recommendations.

## Golden Rules

### 1. Explicit owner consent is mandatory

> **NINGÚN AGENTE, ASISTENTE O AUTOMATIZACIÓN PUEDE MODIFICAR CONFIGURACIONES, CREDENCIALES O SECRETOS SIN MI CONSENTIMIENTO EXPLÍCITO Y PREVIO.**

No agent, assistant or automation may modify protected configuration,
credentials, secrets, data or operational state without the owner's explicit
prior consent.

Protected changes include, among others:

- changing PostgreSQL passwords;
- running `ALTER ROLE` or changing database users and permissions;
- modifying `.env`, `.env.dev`, `.env.test`, `.env.prod` or any secrets file;
- changing environment variables;
- changing database connection credentials;
- changing ports, domains, URLs, certificates or session cookies;
- modifying Docker Compose configuration;
- changing volumes, networks or persistence;
- modifying Spring Boot profiles;
- changing security, authentication or authorization configuration;
- modifying deployment configuration;
- changing user or service permissions.

Before any protected operation, the agent must:

1. Explain what configuration it intends to change.
2. Explain why the change is considered necessary.
3. Identify affected files, services, users or data.
4. Describe risks and consequences.
5. Present the proposed change.
6. Wait for explicit owner authorization.

The following are **not** authorization:

- a generic request to fix a problem;
- a request to review or diagnose an environment;
- previous authorization for a different operation;
- an assumption that the change is necessary;
- the fact that the change appears safe or routine.

Without explicit consent, the agent may only **inspect, diagnose, explain and
propose**. This applies equally to apparently corrective actions such as
synchronizing passwords, regenerating secrets, recreating configurations or
resetting credentials.

### 2. Read the documentation before proposing solutions

Before diagnosing, modifying files or executing any operation, the agent must:

1. Read `master-prompt.md` at the start of every task.
2. Identify and read documentation related to the affected area.
3. Review `docs/environments.md`, architecture, deployment, security,
   database/migration and incident/recovery documentation.
4. Compare the repository state with the documented procedures.
5. Respect existing technical decisions and restrictions.
6. Base diagnoses on project evidence, not generic assumptions.

If documentation is missing, contradictory or outdated:

- do not invent procedures;
- do not assume an undocumented configuration works;
- do not ignore existing instructions;
- inspect the real code, Compose files and configuration;
- distinguish documented facts, verified evidence and hypotheses;
- explain discrepancies;
- propose documentation updates when needed.

Before recommending a solution, the agent must briefly demonstrate that it has
read the applicable documents and understood the restrictions that apply to the
task.

### 3. Never modify the Git repository

- Never run `git add`, `git commit`, `git push`, `git pull`, `git fetch`
  when it changes local state, `git merge`, `git rebase`, `git cherry-pick`,
  `git reset`, `git restore`, `git clean`, `git stash`, `git tag`, or any
  other Git command that mutates the repository or working tree.
- Only read-only Git commands are allowed, such as `git status`, `git diff`,
  `git log`, `git show`, and `git ls-files`.
- The user owns all Git operations.

### 4. Validate environments in strict order: DEV -> TEST -> PROD

- Do not prepare, run, or advance to the next environment until the user has
  explicitly confirmed that the current environment works correctly.
- If a check fails, fix the issue and repeat the checks in the current
  environment.
- Follow [environment-validation.md](environment-validation.md) for the
  required checklist and evidence.
- Automated tests do not replace the manual smoke test of the environment.

### 5. Never run a database migration without explicit owner approval

- Before requesting approval, prepare the migration script and its
  documentation.
- Explain the migration version and filename, purpose, affected tables,
  validation queries, data/backfill impact, and rollback or forward-fix
  strategy.
- Do not run the migration, apply DDL, or restart/recreate a service when
  startup would automatically apply a pending Flyway migration.
- Wait for explicit approval before applying it to any environment.
- Apply migrations in the same strict environment order unless the user
  explicitly directs otherwise.

### 6. Protect PROD above all else

- Never run `docker compose down -v` in PROD.
- Never delete or recreate the PostgreSQL volume to solve backend problems.
- Never run migrations manually without express authorization.
- Never modify production data to facilitate testing.
- Never change passwords to troubleshoot without authorization.
- Never rebuild or deploy PROD services without express authorization.
- Never use DEV or TEST credentials in PROD.
- Never expose secrets in logs, documentation, shared terminals or responses.
- Never assume an installation is empty before executing a destructive
  operation.

### 7. Never expose secrets

- Never print, log, document, commit or return real credentials.
- Do not run commands that dump complete container environments containing
  passwords.
- When inspecting configuration, filter to non-secret keys or redact values.
- If a secret is exposed accidentally, stop and report the exposure to the
  owner without repeating the value.

## Mandatory workflow

1. Inspect the current state using read-only commands.
2. Read the relevant documentation.
3. Prepare code, migration scripts, documentation, and verification commands.
4. Explain any protected change and wait for explicit owner consent.
5. Ask the user to run and validate the current environment.
6. Wait for explicit approval.
7. Only then prepare the next environment.
8. Record evidence without secrets.

## Project references

- [Documentation index](README.md)
- [Architecture](architecture.md)
- [Data model](data-model.md)
- [Environment validation](environment-validation.md)
- [Local environments](environments.md)
- [Installation from Scratch](installation-from-scratch.md)
- [Production Incidents and Recovery](production-incidents-and-recovery.md)
- [Security](security.md)
- [Roadmap](roadmap.md)

## Enforcement summary

If any instruction is ambiguous:

- preserve data first;
- ask the owner before modifying protected state;
- prefer read-only diagnosis;
- do not improvise production changes;
- document the decision and the evidence.
