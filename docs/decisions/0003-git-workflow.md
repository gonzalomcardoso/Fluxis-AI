# ADR-0003: Git Workflow

- **Status:** Accepted
- **Date:** 2026-10-27

## Decision

- Use a single `fluxis-ai` monorepo with `backend/` and `frontend/`.
- Branches: `main` (stable) and `dev` (integration). Daily work happens on `dev`; larger changes use `feat/...` branches merged into `dev`.
- Merge `dev` into `main` at the end of each milestone (milestone 1: day 5).
- Use Conventional Commits for commit messages.
- License: Apache-2.0 (already present in the repository).
- Never commit secrets; only `.env.example`.

## Consequences

- A single CI pipeline for both projects.
- A readable history that is easy to link to plan tasks.
