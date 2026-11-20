# Fluxis AI - Master Prompt

This document defines the highest-priority operating rules for work on Fluxis AI.
If any other instruction, plan, or suggestion conflicts with these rules, these
rules take precedence.

## Golden Rules

1. **Never modify the Git repository.**
   - Never run `git add`, `git commit`, `git push`, `git pull`, `git fetch`
     when it changes local state, `git merge`, `git rebase`, `git cherry-pick`,
     `git reset`, `git restore`, `git clean`, `git stash`, `git tag`, or any
     other Git command that mutates the repository or working tree.
   - Only read-only Git commands are allowed, such as `git status`, `git diff`,
     `git log`, `git show`, and `git ls-files`.
   - The user owns all Git operations.

2. **Validate environments in strict order: DEV -> TEST -> PROD.**
   - Do not prepare, run, or advance to the next environment until the user has
     explicitly confirmed that the current environment works correctly.
   - If a check fails, fix the issue and repeat the checks in the current
     environment.
   - Follow [environment-validation.md](environment-validation.md) for the
     required checklist and evidence.

3. **Never run a database migration without explicit user approval.**
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

## Required workflow

1. Inspect the current state using read-only commands.
2. Prepare code, migration scripts, documentation, and verification commands.
3. Ask the user to run and validate the current environment.
4. Wait for explicit approval.
5. Only then prepare the next environment.

## Project references

- [Architecture](architecture.md)
- [Data model](data-model.md)
- [Environment validation](environment-validation.md)
- [Local environments](environments.md)
- [Security](security.md)
- [Roadmap](roadmap.md)