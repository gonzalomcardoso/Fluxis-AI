# Fluxis AI Documentation Index

Use this index to find the authoritative document before diagnosing,
proposing or executing an operational change.

## Highest-priority rules

- [Master Prompt](master-prompt.md): non-negotiable rules, owner consent,
  environment order, migration approval and PROD protection.
- [Security](security.md): authentication, CSRF, secret handling and protected
  operations.
- [Environment Validation](environment-validation.md): mandatory DEV -> TEST
  -> PROD gates and evidence requirements.

## Environments and operations

- [Local Environments](environments.md): Compose files, ports, volumes,
  profiles, canonical project names and PROD constraints.
- [Installation from Scratch](installation-from-scratch.md): prerequisites,
  clean-machine installation for DEV/TEST/PROD, command classification and
  validation checklist.
- [Production Incidents and Recovery](production-incidents-and-recovery.md):
  real incident records and the official PROD recovery procedure.

## Technical reference

- [Architecture](architecture.md)
- [Data Model](data-model.md)
- [Roadmap](roadmap.md)
- [Decisions / ADRs](decisions/README.md)

## Feature and test data notes

- [Milestone 1](milestone-1.md)
- [Day 8 - CRM](day-8-crm.md)
- [Day 9 - Job Applications](day-9-job-applications.md)
- [Day 10 - Clients and Services](day-10-clients-and-services.md)
- [Day 11 - Proposals](day-11-proposals.md)
- [Day 12 - Subscriptions](day-12-subscriptions.md)
- [Day 13 - Tasks and Audit](day-13-tasks-and-audit.md)
- [Day 14 - Dashboard](day-14-dashboard.md)
- [Option A Test Data](test-data-option-a.md)
- [Option B API Test Data](test-data-option-b.md)
- [Option C Demo Data](test-data-option-c.md)

## Reading order for a new task

1. Read [master-prompt.md](master-prompt.md).
2. Read [security.md](security.md) and the environment-specific document.
3. For PROD, read [production-incidents-and-recovery.md](production-incidents-and-recovery.md).
4. For a clean installation, follow [installation-from-scratch.md](installation-from-scratch.md).
5. Record verified evidence without secrets.
