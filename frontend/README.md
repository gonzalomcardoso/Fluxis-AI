# Frontend de Fluxis AI

## Requisitos

- Node.js 24 LTS (la versión mayor indicada en `.nvmrc`).
- Backend y PostgreSQL disponibles mediante Docker Compose.

## Arranque en desarrollo

Desde la raíz, copia `.env.example` a `.env` si todavía no lo has hecho. En `frontend/`, instala dependencias y arranca Vite:

```powershell
cd frontend
npm install
npm run dev
```

Abre la URL que muestra Vite (por defecto `http://localhost:5173`). El servidor de desarrollo reenvía `/api` a `http://localhost:8080`; no hace falta habilitar CORS en el backend.

`VITE_API_URL=/api` está documentada en el `.env.example` de la raíz y es el valor de desarrollo. Para otros entornos configura `VITE_API_URL` con la ruta base pública de la API, por ejemplo `https://api.example.com/api`. Las variables `VITE_*` se incluyen en el bundle del navegador y nunca deben contener secretos.

## Comprobaciones

Desde `frontend/`:

```powershell
npm run dev
npm test
npm run build
npm run lint
npx tsc --noEmit
```

La aplicación ofrece el listado en `/oportunidades` y el formulario en `/oportunidades/nueva`.
