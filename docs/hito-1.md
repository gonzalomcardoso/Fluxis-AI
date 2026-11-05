# Hito 1: oportunidades laborales, pruebas y CI

Este documento describe cómo levantar la aplicación desde un clon limpio, comprobar la persistencia real y ejecutar las verificaciones locales y de GitHub Actions.

## Requisitos previos

- JDK 25 (Temurin recomendado).
- Node.js 24 LTS y npm.
- Docker Desktop o Docker Engine con Compose y un daemon activo.
- Git.

El backend usa el Maven Wrapper incluido; no es necesario instalar Maven globalmente.

## Arranque desde un clon limpio

### 1. Configuración local y servicios

Desde PowerShell:

```powershell
git clone https://github.com/gonzalomcardoso/Fluxis-AI.git
cd Fluxis-AI
Copy-Item .env.example .env
```

Edita `.env` y sustituye `POSTGRES_PASSWORD` por una contraseña local no reutilizada. No compartas ni confirmes `.env`; el archivo está ignorado por Git. Las variables `VITE_*` son públicas y no deben contener secretos.

Levanta PostgreSQL y el backend:

```powershell
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Espera hasta que ambos servicios estén saludables antes de continuar.

### 2. Arranque del frontend

En una segunda terminal:

```powershell
cd C:\ruta\al\clon\Fluxis-AI\frontend
npm ci
npm run dev
```

Abre la URL que muestra Vite, normalmente `http://localhost:5173`. El proxy de desarrollo reenvía `/api` al backend en `localhost:8080`.

### 3. Crear y verificar una oportunidad

En la interfaz, abre **Añadir oportunidad**, completa empresa y puesto, y guarda. La oportunidad debe aparecer en el listado. Recarga la página para confirmar que vuelve desde el backend.

También se puede comprobar la API desde PowerShell:

```powershell
$body = @{
  companyName = "Empresa de prueba"
  title = "Desarrollador/a de plataforma"
  url = "https://jobs.example.test/fluxis-hito-1"
  source = "Prueba local"
  technologies = @("Java", "PostgreSQL")
  status = "PENDING"
} | ConvertTo-Json

$created = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/job-opportunities `
  -ContentType "application/json" `
  -Body $body

$created
Invoke-RestMethod "http://localhost:8080/api/job-opportunities/$($created.id)"
Invoke-RestMethod "http://localhost:8080/api/job-opportunities?page=0&size=10"
```

Para comprobar la fila persistida directamente en PostgreSQL (con los valores predeterminados de `.env.example`):

```powershell
docker compose exec postgres psql -U fluxis_dev -d fluxis_dev `
  -c "SELECT id, company_name, title, status FROM job_opportunities ORDER BY created_at DESC LIMIT 10;"
```

Si cambiaste `POSTGRES_DEV_USER` o `POSTGRES_DEV_DB`, usa esos valores en el comando. Los ejemplos usan datos sintéticos; borra la oportunidad de prueba desde la base local si ya no la necesitas. Consulta [Ambientes dev y test](./environments.md) para el servicio manual de test y los procedimientos de reset sin afectar la otra base.

## Pruebas locales

### Backend

Desde `backend/`:

```powershell
.\mvnw.cmd test
```

Ejecuta unitarias y web sin PostgreSQL. Para incluir integración con el contenedor PostgreSQL 18, con Docker activo:

```powershell
.\mvnw.cmd verify
```

La convención es `*Test` para Surefire y `*IT` para Failsafe. Detalle: [README del backend](../backend/README.md).

### Frontend

Desde `frontend/`:

```powershell
npm ci
npm run test:run
npm run lint
npm exec -- tsc --noEmit
npm run build
```

Las pruebas de componentes usan Vitest y React Testing Library en `jsdom`. El acceso a la API se sustituye en la frontera del módulo de API con mocks de Vitest; no se añade MSW porque el objetivo de estas pruebas es el comportamiento de componentes/hooks y la capa HTTP ya está centralizada.

## GitHub Actions

El workflow [CI](../.github/workflows/ci.yml) corre en `push` y `pull_request` hacia `dev`, `test`, `prod` y `main`. No hace falta configurar un GitHub Environment, variables secretas ni servicios PostgreSQL: el runner Ubuntu provee Docker y Testcontainers levanta PostgreSQL 18 para las pruebas backend.

Los jobs backend y frontend se ejecutan en paralelo. El backend usa Java 25 y ejecuta `./mvnw verify`; el frontend usa la versión de `.nvmrc` y ejecuta instalación reproducible, lint, typecheck, pruebas no interactivas y build. Si falla una prueba, se suben los informes Surefire/Failsafe o JUnit como artefactos para diagnosticar el run.

Para ver el resultado en GitHub, publica la rama:

```powershell
git push origin test
```

Si la rama remota `test` todavía no existe, créala desde el commit que quieras verificar y publícala con `git switch -c test` y `git push -u origin test`. El workflow también se ejecuta en `dev`, `prod` y `main`. El estado de `dev` se muestra en el badge de CI del [README raíz](../README.md).

## Checklist del hito

- [ ] PostgreSQL y backend arrancan saludables desde Compose.
- [ ] Se crea una oportunidad desde la interfaz.
- [ ] La oportunidad permanece tras recargar y se consulta por API.
- [ ] La fila aparece en PostgreSQL.
- [ ] Backend: `.\mvnw.cmd verify` pasa con Testcontainers/PostgreSQL 18.
- [ ] Frontend: tests, lint, typecheck y build pasan.
- [ ] El workflow CI pasa en GitHub para la rama publicada.

## Limitaciones conocidas

- Todavía no hay autenticación real; el backend usa el usuario de desarrollo.
- No se implementan Gmail, IA, edición ni borrado de oportunidades.
- Las pruebas de integración backend requieren Docker; las unitarias y web no.
- CI prueba los cambios; no despliega la aplicación.
