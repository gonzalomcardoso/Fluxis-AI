# Fluxis AI Frontend

## Requirements

- Node.js 24 LTS (the major version specified in `.nvmrc`).
- Backend and PostgreSQL available through Docker Compose.

## Development

From the repository root, copy `.env.example` to `.env` if you have not already done so. From `frontend/`, install dependencies and start Vite:

```powershell
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite (default: `http://localhost:5173`). The development server forwards `/api` to `http://localhost:8080`; there is no need to enable CORS on the backend.

`VITE_API_URL=/api` is documented in the root `.env.example` and is the development value. For other environments, set `VITE_API_URL` to the public API base path, for example `https://api.example.com/api`. `VITE_*` variables are included in the browser bundle and must never contain secrets.

## Checks

From `frontend/`:

```powershell
npm ci
npm run test:run
npm run build
npm run lint
npm exec -- tsc --noEmit
```

The application provides the opportunity list at `/oportunidades` and the form at `/oportunidades/nueva`.
