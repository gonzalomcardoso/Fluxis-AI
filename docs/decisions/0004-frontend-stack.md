# ADR-0004: Frontend stack

- **Status:** Accepted
- **Date:** 2026-11-03
- **Context:** Day 4 of the Fluxis AI implementation plan.

## Decision

Use the following stable package versions, verified against npm registry metadata on the decision date:

| Area | Versions | Reason |
|---|---|---|
| Runtime | Node.js 24 LTS (`.nvmrc`) | Existing repository runtime decision; Vite 8 and Vitest 5 support Node 24. |
| UI and build | React/React DOM 19.3.1, Vite 8.3.5, `@vitejs/plugin-react` 6.2.1 | The React plugin declares Vite 8 compatibility; matches the repository's React 19 and Vite 8 stack. |
| Types | TypeScript 6.0.3, `@types/react`/`react-dom` 19.3.0 | Preserves the existing ADR-0001 TypeScript 6 decision with strict checking. |
| CSS | Tailwind CSS and `@tailwindcss/vite` 4.3.4 | Uses Tailwind's official Vite plugin and CSS-first `@import "tailwindcss"` integration. The plugin supports Vite 8. |
| Routing and server state | React Router 7.18.5, TanStack Query 5.105.3 | React Router remains on the major selected in ADR-0001; Query manages backend data and cache invalidation. |
| Forms and validation | React Hook Form 7.91.0, Zod 4.6.7, `@hookform/resolvers` 7.0.2 | Resolver metadata explicitly supports both Zod 3 and Zod 4 and React Hook Form 7. |
| Verification | Vitest 5.1.0, React Testing Library 16.3.3, jest-dom 7.0.1, user-event 14.6.8, jsdom 30.1.3 | Vitest supports Vite 8; RTL provides accessible DOM rendering and assertions. |
| Linting | ESLint 10.14.0, typescript-eslint 8.72.1, eslint-plugin-react-hooks 7.1.1 | Current flat-config lint setup with TypeScript and Hooks rules. typescript-eslint supports TypeScript 6.0 and ESLint 10. |

No component library is added. A small set of native, accessible controls styled with Tailwind is sufficient for the MVP.

## Consequences

- Vite development requests use `/api` and are proxied to the local backend, so this setup does not require CORS changes.
- `VITE_API_URL` is public browser configuration; secrets must never use the `VITE_` prefix.
- The backend client stays centralized so authentication headers can later be added in one place.
