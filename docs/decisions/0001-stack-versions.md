# ADR-0001: Technology Stack Versions

- **Status:** Accepted
- **Date:** 2026-10-27
- **Context:** project kickoff (day 1). The priority is a stable, long-term-supported foundation, not simply the newest version.

## Decision

### Backend and database

| Technology | Decision | Rationale | Source consulted |
|---|---|---|---|
| Java | **25 LTS** (Temurin) | Latest LTS (Sep 2025). Java 26 is a non-LTS release. Spring Boot 4.x supports Java 17 through 26. | Java and Spring version guides |
| Spring Boot | **4.1.x** (latest release: 4.1.2, 22 Oct 2026) | Latest stable line. 4.2.0 is a release candidate expected in November 2026, so an RC is not used. Boot 3.5 no longer receives open-source patches. | spring-boot releases on GitHub; spring.io |
| Spring Framework / Security / Data | Versions managed by the Boot 4.1 BOM (Framework 7.0.x, Security 7.1, Data 2026.0) | Do not pin manually | InfoQ |
| Spring AI | **2.0.x** (2.0.0 GA on 12 Jun 2026) | Designed for Boot 4.0/4.1. Requires Boot 4 (Jackson 3). | spring.io; GitHub spring-ai |
| Flyway, Testcontainers, JUnit, Mockito | Versions from the Boot BOM | Avoids incompatibilities | - |
| PostgreSQL | **18** (image `postgres:18`) | Supported through Nov 2030. PostgreSQL 19 is not yet considered final in the sources consulted. | cloudnative-pg/postgres-containers |
| Build | **Maven** with Maven Wrapper | Fewer concepts to learn than with Gradle | - |

### Front end

| Technology | Decision | Rationale |
|---|---|---|
| Node.js | **24 LTS** (`.nvmrc`) | Supported through 30 Apr 2028. Node 26 enters LTS on 28 Oct 2026; it will be reevaluated in a future ADR. |
| React | **19.x** | Current stable line. One source reports 19.3.0 (Sep 2026); pin the latest with `npm view react version`. |
| TypeScript | **6.0.x**, `strict` mode | TypeScript 7.0 (native Go compiler) has been released, but some tools (e.g. typescript-eslint) still depend on the 6.0 API. Migration to 7 is planned in a separate ADR. |
| Vite | **8.x** | Requires Node 20.19+ or 22.12+. Compatible with Node 24. |
| Vitest | **4.1+** | Compatible with Vite 8. |
| Tailwind CSS | **4.x** | CSS-first configuration (`@import "tailwindcss"`). |
| React Router, TanStack Query, React Hook Form, Zod | Latest stable major versions (React Router 7, TanStack Query 5, React Hook Form 7, Zod 4) | **Not verified today**: pin these when installing them on day 4 with `npm view <package> version`. |
| Package manager | **npm** | Less initial configuration |

## What could not be verified

The work environment cannot access package registries, so **patch** versions (for example, 4.1.2 or 19.3.x) are based on web searches and must be confirmed with the commands below before pinning them. Major and minor lines have been cross-checked.

## How to re-verify

```bash
# Installed Java and Node versions
java -version && node -v

# Front end (day 4)
npm view react version
npm view typescript@6 version
npm view vite version
npm view vitest version
npm view tailwindcss version

# Backend (day 2): generate the project with start.spring.io using Boot 4.1.x
# and check the resulting version in pom.xml
```

## Consequences

- Spring AI 2.0 and Boot 4 use Jackson 3 and JSpecify annotations; older Boot 3 sample code may not compile.
- Review versions at the end of week 4 and whenever a new major version is released.
