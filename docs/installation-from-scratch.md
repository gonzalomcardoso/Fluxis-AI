# Installation from Scratch: DEV, TEST and PROD

**Estado:** guía operativa.
**Alcance:** instalar Fluxis AI desde una máquina sin Docker, PostgreSQL, contenedores, imágenes ni bases de datos previas.
**Última revisión documental:** 2026-11-20.

Esta guía se basa en los archivos reales del repositorio: `compose.dev.yaml`, `compose.test.yaml`, `compose.prod.yaml`, `backend/Dockerfile`, los perfiles Spring Boot, `.nvmrc`, `frontend/package.json` y las migraciones Flyway `V1`-`V14`.

> **REGLA DE ORO:** ningún agente puede modificar configuraciones, credenciales, secretos, volúmenes, servicios ni datos sin consentimiento explícito y previo del propietario. Esta guía documenta comandos, pero no autoriza su ejecución.

> **PROD no se inicializa desde cero si ya contiene datos.** Antes de ejecutar cualquier paso de la sección PROD, confirmar que se trata de una instalación nueva o usar [production-incidents-and-recovery.md](production-incidents-and-recovery.md).

## 1. Clasificación de comandos

| Etiqueta | Significado |
|---|---|
| `[READ-ONLY]` | Inspecciona sin modificar servicios, datos ni configuración. |
| `[CONFIG]` | Crea o modifica archivos de entorno, secretos o configuración. Requiere autorización. |
| `[BUILD]` | Construye imágenes; no borra datos, pero consume recursos. |
| `[START]` / `[RESTART]` | Crea, inicia o recrea contenedores. En PROD requiere autorización. |
| `[DATA]` | Ejecuta migraciones, DML o cambios de credenciales. Requiere autorización. |
| `[DESTRUCTIVE]` | Puede borrar volúmenes o datos. Prohibido en PROD sin autorización excepcional y backup verificado. |

## 2. Compatibilidad y evidencias

El repositorio no define una matriz oficial de sistemas operativos. La ruta documentada y verificada por el proyecto usa **Windows + PowerShell**. Las variantes macOS/Linux son adaptaciones de las mismas herramientas y deben considerarse **no verificadas en este repositorio**.

| Sistema | Estado documental | Requisito práctico |
|---|---|---|
| Windows 10/11 x64 | Ruta principal documentada | Docker Desktop con backend WSL 2, PowerShell 5.1+ |
| macOS reciente | Adaptación no verificada | Docker Desktop para macOS |
| Linux x86_64 | Adaptación no verificada | Docker Engine + Docker Compose plugin |

El backend se ejecuta en contenedores Linux. El frontend no está incluido en los Compose: se ejecuta con Node.js en el host o se publica como build estático.

## 3. Requisitos previos

### 3.1 Versiones

| Herramienta | Versión requerida | Fuente en repositorio |
|---|---|---|
| Java | JDK 25 (Temurin recomendado) | `backend/pom.xml`, README, CI |
| Maven | Wrapper incluido; no requiere Maven global | `backend/mvnw`, `backend/mvnw.cmd` |
| Node.js | 24 LTS | `.nvmrc` |
| npm | Incluido con Node.js | `frontend/package-lock.json` |
| Docker Desktop / Engine | Con Docker Compose plugin y daemon activo | Compose y CI |
| PostgreSQL | 18, ejecutado en contenedor | `postgres:18` en los Compose |
| Git | Cualquier versión reciente | Clonado del repositorio |
| PowerShell | 5.1 o superior | Comandos de esta guía |

### 3.2 Qué necesita cada modalidad

| Modalidad | Herramientas necesarias |
|---|---|
| Todo el backend y PostgreSQL en Docker | Git, Docker Desktop/Engine + Compose, Node.js/npm para el frontend |
| Backend local y tests Maven en el host | Además: JDK 25; Maven Wrapper incluido |
| PostgreSQL local en el host | No es necesario para esta guía; se usa PostgreSQL en Docker |

### 3.3 Hardware y disco

El repositorio no fija requisitos mínimos de hardware. Recomendación operativa prudente, no un requisito formal:

- 8 GB de RAM como mínimo práctico; 16 GB recomendado.
- 4 CPU virtuales.
- 25-30 GB libres para imágenes, contenedores, dependencias Maven/npm y volúmenes.
- Virtualización habilitada en BIOS/UEFI si se usa Docker Desktop con WSL 2.

### 3.4 Comprobar herramientas instaladas

```powershell
# [READ-ONLY]
git --version
java -version
node --version
npm --version
docker --version
docker compose version
docker info
.\backend\mvnw.cmd -version
```

Resultados esperados:

- Java muestra versión 25.
- Node muestra una versión 24.x.
- `docker compose version` responde; no debe confundirse con `docker-compose` antiguo.
- `docker info` termina sin error de daemon.
- Maven Wrapper muestra la versión de Maven descargada.

### 3.5 Instalación desde cero en Windows

Instalar desde fuentes oficiales:

- Git: <https://git-scm.com/downloads>
- Docker Desktop: <https://www.docker.com/products/docker-desktop/>
- Temurin JDK 25: <https://adoptium.net/temurin/releases/?version=25>
- Node.js 24 LTS: <https://nodejs.org/en/download>

Alternativas con `winget` (verificar el identificador antes de instalar):

```powershell
# [CONFIG DEL SISTEMA - ejecutar fuera de esta guía si el propietario lo autoriza]
winget search Git
winget search Docker
winget search Temurin
winget search NodeJS
```

Después de instalar Docker Desktop:

1. Abrir Docker Desktop.
2. Activar WSL 2 si Windows lo solicita.
3. Esperar hasta que el motor muestre **Engine running**.
4. Comprobar con `docker info`.

Pasos de instalación recomendados en Windows:

1. Git: ejecutar el instalador oficial y aceptar las opciones predeterminadas; al finalizar, abrir una terminal nueva y comprobar git --version.
2. Docker Desktop: ejecutar el instalador, habilitar WSL 2 si se solicita, reiniciar si corresponde y esperar a que el motor esté en ejecución; comprobar docker info y docker compose version.
3. JDK 25: instalar Temurin 25 desde el instalador oficial; configurar JAVA_HOME si el instalador no lo hace y verificar java -version.
4. Node.js: instalar la versión 24 LTS (no una versión LTS posterior sin actualizar .nvmrc) y verificar 
ode --version y 
pm --version.
5. Maven: no instalar globalmente; usar ackend\mvnw.cmd.

Si WSL 2 no está disponible, ejecutar wsl --install en una PowerShell elevada, reiniciar y volver a abrir Docker Desktop. Esta operación modifica el sistema operativo y requiere autorización del propietario.

No es necesario instalar PostgreSQL en Windows: los tres entornos usan PostgreSQL 18 en contenedores.

### 3.6 macOS y Linux

La aplicación y los Compose son compatibles con Docker Linux. Sin embargo, esta guía no conserva evidencia de una instalación completa en macOS/Linux.

- macOS: instalar Docker Desktop y Node.js 24; usar una terminal compatible con comandos POSIX para copiar archivos y ejecutar Compose.
- Linux: instalar Docker Engine y el plugin `docker compose`; instalar Node.js 24 y Git. Agregar el usuario al grupo Docker solo si la política de seguridad local lo permite.
- En macOS/Linux, los ejemplos que usan `Copy-Item` o variables `$env:NAME` deben adaptarse a `cp` y variables de shell POSIX. No se documentan como comandos verificados.

## 4. Preparar el repositorio

```powershell
# [READ-ONLY] Elegir una carpeta de trabajo.
Set-Location C:\Users\pudre\Documents\GitHub

# [CONFIG] Clonar el repositorio si aún no existe.
git clone https://github.com/gonzalomcardoso/Fluxis-AI.git
Set-Location .\Fluxis-AI
```

Comprobar los archivos que definen la instalación:

```powershell
# [READ-ONLY]
Get-ChildItem compose.*.yaml, .env.*.example, .nvmrc
Get-Content backend\src\main\resources\application.yml
Get-Content frontend\package.json
```

### 4.1 Archivos de entorno

Cada entorno tiene su propio archivo local, ignorado por Git:

| Entorno | Plantilla | Archivo local |
|---|---|---|
| DEV | `.env.dev.example` | `.env.dev` |
| TEST | `.env.test.example` | `.env.test` |
| PROD | `.env.prod.example` | `.env.prod` |

Los archivos reales nunca se versionan. No copiar un `.env` de DEV a PROD ni reutilizar contraseñas.

```powershell
# [READ-ONLY] Verificar que los archivos locales estén ignorados.
git check-ignore -v .env.dev .env.test .env.prod
```

### 4.2 Crear secretos correctamente

La generación de contraseñas es una operación protegida cuando afecta a PROD. El propietario debe aprobar y generar un valor único. Ejemplo de generación local, sin guardar el resultado en documentación:

```powershell
# [CONFIG] Generar un valor aleatorio; no imprimirlo en terminales compartidas.
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Nunca usar el valor de DEV o TEST en PROD. No pegar secretos en tickets, chats, logs ni commits.

## 5. Entorno DEV desde cero

### 5.1 Variables DEV

```powershell
# [CONFIG] Crear el archivo local desde la plantilla.
Copy-Item .env.dev.example .env.dev
```

Editar `.env.dev` y sustituir el placeholder de contraseña. Variables reales:

```dotenv
POSTGRES_DEV_DB=fluxis_dev
POSTGRES_DEV_USER=fluxis_dev
POSTGRES_DEV_PASSWORD=<unico-y-local>
POSTGRES_DEV_PORT=5432
SERVER_PORT=8080
APP_FRONTEND_URL=http://localhost:5173
DEV_SEED_PASSWORD=<password-del-seeder-dev>
```

No usar el placeholder de la plantilla como credencial real.

### 5.2 Validar y construir la imagen DEV

Desde la raíz del repositorio:

```powershell
# [READ-ONLY] Validar interpolación del Compose.
docker compose --env-file .env.dev --file compose.dev.yaml config --quiet

# [BUILD] Construir la imagen etiquetada que usa compose.dev.yaml.
docker build --file backend\Dockerfile --tag fluxis-ai-backend:env-dev backend
```

La imagen se construye desde `backend/Dockerfile` con contexto `backend/`.

### 5.3 Crear PostgreSQL y arrancar el backend

```powershell
# [START] Crear red, volumen y contenedores; Flyway se ejecuta al iniciar backend.
docker compose --env-file .env.dev --file compose.dev.yaml up -d

# [READ-ONLY] Ver estado y healthchecks.
docker compose --env-file .env.dev --file compose.dev.yaml ps

# [READ-ONLY] Esperar a que devuelva {"status":"UP"}.
Invoke-RestMethod http://localhost:8083/actuator/health
```

Al inicializarse un volumen vacío, PostgreSQL crea la base y el rol definidos en `.env.dev`. Al arrancar el backend, Flyway aplica las migraciones `V1`-`V14`. El perfil `dev` activa `DevDataSeeder`, que crea los usuarios sintéticos:

- `dev@fluxis.local`
- `demo@fluxis.local`

Ambos usan `DEV_SEED_PASSWORD`.

### 5.4 Arrancar el frontend DEV

En otra terminal:

```powershell
# [READ-ONLY] Instalación reproducible de dependencias.
Set-Location frontend
npm ci

# [START] Servidor Vite en 127.0.0.1:5173.
npm run dev
```

Abrir `http://localhost:5173`. El proxy de Vite reenvía `/api` a `http://localhost:8083`.

### 5.5 Validación funcional DEV

1. Abrir la aplicación.
2. Iniciar sesión con `dev@fluxis.local` y `DEV_SEED_PASSWORD`.
3. Verificar que se listan oportunidades.
4. Crear una oportunidad desde la UI.
5. Recargar y confirmar que persiste.
6. Consultar la base con una orden de solo lectura:

```powershell
# [READ-ONLY]
docker compose --env-file .env.dev --file compose.dev.yaml exec postgres psql -U fluxis_dev -d fluxis_dev -c "SELECT id, company_name, title, status FROM job_opportunities ORDER BY created_at DESC LIMIT 10;"
```

### 5.6 Detener y volver a levantar DEV conservando datos

```powershell
# [RESTART] Detener sin eliminar contenedores ni volúmenes.
docker compose --env-file .env.dev --file compose.dev.yaml stop

# [RESTART] Volver a iniciar los contenedores existentes.
docker compose --env-file .env.dev --file compose.dev.yaml start
```

Alternativa para recrear contenedores conservando el volumen nombrado:

```powershell
# [RESTART] Elimina contenedores y red, pero conserva el volumen nombrado.
docker compose --env-file .env.dev --file compose.dev.yaml down
docker compose --env-file .env.dev --file compose.dev.yaml up -d
```

No ejecutar `down -v`. No borrar `fluxis-ai_postgres_dev_data`.

## 6. Entorno TEST desde cero

TEST es un entorno aislado. Nunca debe apuntar a la base de PROD ni reutilizar sus credenciales.

### 6.1 Variables TEST

```powershell
# [CONFIG] Crear archivo local TEST.
Copy-Item .env.test.example .env.test
```

Variables reales:

```dotenv
POSTGRES_TEST_DB=fluxis_test
POSTGRES_TEST_USER=fluxis_test
POSTGRES_TEST_PASSWORD=<unico-y-local-test>
POSTGRES_TEST_PORT=5433
TEST_SEED_PASSWORD=<password-del-seeder-test>
TEST_SERVER_PORT=8084
```

### 6.2 Validar, construir y arrancar TEST

```powershell
# [READ-ONLY]
docker compose --env-file .env.test --file compose.test.yaml config --quiet

# [BUILD]
docker build --file backend\Dockerfile --tag fluxis-ai-backend:env-test backend

# [START]
docker compose --env-file .env.test --file compose.test.yaml up -d

# [READ-ONLY]
docker compose --env-file .env.test --file compose.test.yaml ps
Invoke-RestMethod http://localhost:8084/actuator/health
```

El Compose TEST configura:

- perfil Spring Boot `test,test-data`;
- PostgreSQL exclusivo `fluxis_test`;
- puerto host `5433`;
- backend host `8084`;
- `TEST_SEED_PASSWORD`;
- volumen `fluxis-ai_postgres_test_data`.

El perfil `test-data` activa `ManualTestDataSeeder`, que crea `integration@fluxis.local` y datos sintéticos de oportunidades.

### 6.3 Frontend TEST

```powershell
# [READ-ONLY] Dependencias.
Set-Location frontend
npm ci

# [START] Vite en 127.0.0.1:5174.
npm run dev:test
```

Abrir `http://localhost:5174`. El proxy reenvía `/api` a `http://localhost:8084`.

### 6.4 Pruebas automatizadas

Backend, desde `backend/`:

```powershell
# [READ-ONLY respecto de la base manual] Los tests usan Testcontainers.
.\mvnw.cmd test
.\mvnw.cmd verify
```

Frontend, desde `frontend/`:

```powershell
# [READ-ONLY respecto de la base manual]
npm run test:run
npm run lint
npm exec -- tsc --noEmit
npm run build
```

Los tests de Testcontainers no usan la base de `compose.test.yaml`; crean PostgreSQL efímero.

### 6.5 Validación de datos TEST

```powershell
# [READ-ONLY] Verificar usuario sintético TEST.
docker compose --env-file .env.test --file compose.test.yaml exec postgres_test psql -U fluxis_test -d fluxis_test -c "SELECT email, display_name FROM users WHERE email = 'integration@fluxis.local';"

# [READ-ONLY] Verificar historial Flyway.
docker compose --env-file .env.test --file compose.test.yaml exec postgres_test psql -U fluxis_test -d fluxis_test -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

### 6.6 Detener y recuperar TEST

```powershell
# [RESTART] Detener conservando datos.
docker compose --env-file .env.test --file compose.test.yaml stop

# [RESTART] Volver a iniciar.
docker compose --env-file .env.test --file compose.test.yaml start
```

Para recrear contenedores conservando el volumen:

```powershell
# [RESTART] down sin -v conserva el volumen nombrado.
docker compose --env-file .env.test --file compose.test.yaml down
docker compose --env-file .env.test --file compose.test.yaml up -d
```

**TEST nunca debe usar la base de PROD.** No cambiar `SPRING_DATASOURCE_URL` de TEST para apuntar a `postgres_prod`.

## 7. Entorno PROD desde cero

> **ADVERTENCIA:** esta sección solo aplica a una máquina limpia y a una instalación PROD nueva. Si el volumen `fluxis-ai_postgres_prod_data` ya existe o contiene datos, detenerse y usar [production-incidents-and-recovery.md](production-incidents-and-recovery.md). Nunca inicializar desde cero sobre una instalación existente.

Antes de comenzar, el propietario debe autorizar:

- creación de `.env.prod` y de credenciales únicas;
- construcción de la imagen PROD;
- creación del volumen y arranque de servicios;
- aplicación inicial de Flyway `V1`-`V14`;
- creación de la cuenta inicial.

### 7.1 Preparar una máquina limpia

1. Instalar Git, Docker Desktop/Engine, Node.js 24 y, si se ejecutarán tests locales, JDK 25.
2. Verificar que Docker tenga el daemon activo.
3. Confirmar que no existen contenedores o volúmenes previos de Fluxis AI:

```powershell
# [READ-ONLY]
docker ps -a
docker volume ls
```

Si aparece `fluxis-ai_postgres_prod_data`, no continuar con esta sección sin revisar el procedimiento de recuperación.

### 7.2 Variables PROD

```powershell
# [CONFIG] Crear el archivo local desde la plantilla.
Copy-Item .env.prod.example .env.prod
```

La plantilla contiene:

```dotenv
POSTGRES_PROD_DB=fluxis_prod
POSTGRES_PROD_USER=fluxis_prod
POSTGRES_PROD_PASSWORD=<generar-valor-unico-y-protegido>
PROD_SERVER_PORT=8085
APP_PROD_FRONTEND_URL=http://localhost:5175
SESSION_COOKIE_SECURE=false
```

Criterios:

- `POSTGRES_PROD_PASSWORD` debe ser único y no reutilizado de DEV/TEST.
- El valor real debe guardarse en un gestor de secretos, no en Git.
- `PROD_SERVER_PORT` debe coincidir con el puerto usado por el frontend/proxy.
- `SESSION_COOKIE_SECURE=false` solo es válido para la simulación local HTTP. Detrás de HTTPS debe ser `true`.
- No publicar el puerto de PostgreSQL: `compose.prod.yaml` no lo publica.
- No cambiar variables de seguridad, cookies, puertos o URLs sin autorización del propietario.

> Nota de coherencia: la documentación y el valor por defecto de `compose.prod.yaml` usan `8085`. Si un archivo local o plantilla contiene otro puerto, revisarlo como configuración local antes de arrancar; no cambiarlo automáticamente.

### 7.3 Validar y construir PROD

```powershell
# [READ-ONLY] Validar interpolación y modelo Compose.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml config --quiet

# [BUILD] Construir la imagen desde ./backend usando backend/Dockerfile.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml build backend_prod
```

El Dockerfile compila con Maven/JDK 25 y produce `/app/app.jar` dentro de una imagen JRE 25.

### 7.4 Crear PostgreSQL y el volumen persistente

```powershell
# [START] Crear red y volumen; inicializar PostgreSQL.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml up -d postgres_prod

# [READ-ONLY]
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml ps postgres_prod
docker volume inspect fluxis-ai_postgres_prod_data
```

El volumen `fluxis-ai_postgres_prod_data` se crea una sola vez. No se debe eliminar para reinicializar; si ya existe, el comando no lo vacía ni cambia la contraseña del rol.

### 7.5 Arrancar backend y aplicar Flyway inicial

```powershell
# [START + DATA] Al arrancar, Flyway aplica migraciones pendientes.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml up -d backend_prod

# [READ-ONLY]
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml ps
Invoke-RestMethod http://localhost:8085/actuator/health
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml logs --tail=200 backend_prod
```

Criterios:

- el backend queda `UP`;
- los logs muestran Flyway sin errores;
- el historial llega a `V14`;
- no hay migraciones fallidas.

Verificación de solo lectura:

```powershell
# [READ-ONLY]
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

### 7.6 Crear la cuenta inicial PROD

Solo si la tabla `users` está vacía. La creación de cuenta es una operación protegida y requiere autorización.

```powershell
# [DATA] Comando interactivo; no escribe la contraseña en el historial.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-bootstrap --spring.main.web-application-type=none
```

Solicita email, nombre y contraseña. No volver a ejecutar `prod-bootstrap` si la cuenta ya existe.

Comandos disponibles para operaciones posteriores, siempre con autorización:

```powershell
# [DATA] Reset interactivo de contraseña.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-reset --spring.main.web-application-type=none

# [READ-ONLY] Comprobación interactiva contra el hash BCrypt.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml run --build --rm -it --no-deps --entrypoint java backend_prod -jar /app/app.jar --spring.profiles.active=prod,prod-password-check --spring.main.web-application-type=none
```

### 7.7 Frontend PROD local

```powershell
# [READ-ONLY] Instalar dependencias.
Set-Location frontend
npm ci

# [START] Servidor Vite con mode=prod en 127.0.0.1:5175.
npm run dev:prod
```

Abrir `http://localhost:5175`. El proxy por defecto apunta a `http://localhost:8085`.

Para una publicación real, `VITE_API_URL` debe apuntar al API público y el frontend debe servirse detrás de HTTPS. `VITE_*` no debe contener secretos.

### 7.8 Seguridad PROD

Comprobar que el perfil `prod`:

- desactiva registro público;
- no ejecuta seeders DEV/TEST;
- usa `SESSION_COOKIE_SECURE` según el contexto;
- mantiene CSRF ligado a la sesión;
- no publica PostgreSQL.

Pruebas mínimas:

```powershell
# [READ-ONLY] Health.
Invoke-RestMethod http://localhost:8085/actuator/health

# [READ-ONLY] Endpoint protegido sin sesión: debe rechazar el acceso.
try { Invoke-RestMethod http://localhost:8085/api/dashboard } catch { $_.Exception.Response.StatusCode.value__ }
```

Comprobar que no existan usuarios sintéticos locales:

```powershell
# [READ-ONLY]
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM users WHERE email LIKE ''%@fluxis.local'';"'
```

Resultado esperado: `0`.

Prueba funcional autenticada:

1. Abrir `http://localhost:5175`.
2. Iniciar sesión con la cuenta PROD creada en el bootstrap.
3. Confirmar que el dashboard carga sin `404`.
4. Confirmar que las pantallas de candidaturas y módulos principales cargan.
5. Verificar que no hay datos sintéticos visibles.
6. No crear datos de prueba en PROD salvo autorización explícita.

No registrar la contraseña en el historial de PowerShell. Si se usa la API para
la prueba, obtener primero el token CSRF de `/api/auth/csrf`, iniciar sesión con
una `WebRequestSession` y reutilizar la cookie de sesión.

### 7.9 Comprobar persistencia después de reiniciar

```powershell
# [RESTART] Recrear solo el backend, conservando PostgreSQL.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml up -d --no-deps --force-recreate backend_prod

# [READ-ONLY]
Invoke-RestMethod http://localhost:8085/actuator/health
```

Después de reiniciar, verificar que los datos de prueba creados por el usuario siguen presentes a través de la aplicación. No crear datos de prueba si no está autorizado.

### 7.10 Parada y recuperación PROD

```powershell
# [RESTART] Detener sin eliminar volúmenes.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml stop

# [RESTART] Volver a iniciar.
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml start
```

Para recuperar una instalación existente, no volver a ejecutar la sección de inicialización. Usar [production-incidents-and-recovery.md](production-incidents-and-recovery.md).

## 8. Matriz comparativa de entornos

| Característica | DEV | TEST | PROD |
|---|---|---|---|
| Perfil Spring Boot | `dev` | `test,test-data` | `prod` |
| Compose | `compose.dev.yaml` | `compose.test.yaml` | `compose.prod.yaml` |
| Variables | `.env.dev` | `.env.test` | `.env.prod` |
| Proyecto Compose | `fluxis-env-dev` | `fluxis-env-test` | `fluxis-env-prod` |
| Base de datos | `fluxis_dev` | `fluxis_test` | `fluxis_prod` |
| Backend host | `8083` | `8084` | `8085` |
| Frontend host | `5173` | `5174` | `5175` |
| PostgreSQL host | `5432`, loopback | `5433`, loopback | No publicado |
| Volumen | `fluxis-ai_postgres_dev_data` | `fluxis-ai_postgres_test_data` | `fluxis-ai_postgres_prod_data` |
| Seeder | `dev` | `test-data` | Ninguno |
| Registro público | Habilitado por defecto | Habilitado por defecto | Deshabilitado |
| Credenciales | Únicas DEV | Únicas TEST | Únicas PROD |
| `down -v` | Evitar | Evitar | Prohibido |

## 9. Comandos de ciclo de vida

### 9.1 Levantar

```powershell
# DEV
docker compose --env-file .env.dev --file compose.dev.yaml up -d

# TEST
docker compose --env-file .env.test --file compose.test.yaml up -d

# PROD (requiere autorización)
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml up -d --build
```

### 9.2 Detener

```powershell
# DEV
docker compose --env-file .env.dev --file compose.dev.yaml stop

# TEST
docker compose --env-file .env.test --file compose.test.yaml stop

# PROD (requiere autorización)
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml stop
```

### 9.3 Reconstruir solo el backend

```powershell
# DEV
docker build --file backend\Dockerfile --tag fluxis-ai-backend:env-dev backend
docker compose --env-file .env.dev --file compose.dev.yaml up -d --no-deps --force-recreate backend_env_dev

# TEST
docker build --file backend\Dockerfile --tag fluxis-ai-backend:env-test backend
docker compose --env-file .env.test --file compose.test.yaml up -d --no-deps --force-recreate backend_env_test

# PROD (requiere autorización)
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml build --no-cache backend_prod
docker compose --project-name fluxis-env-prod --env-file .env.prod --file compose.prod.yaml up -d --no-deps --force-recreate backend_prod
```

### 9.4 Operaciones destructivas

```text
[PROHIBIDO EN PROD SIN AUTORIZACION EXCEPCIONAL Y BACKUP VERIFICADO]
docker compose down -v
docker volume rm
docker system prune --volumes
```

No se incluye ningún procedimiento destructivo como solución rutinaria.

## 10. Validación final de una instalación limpia

### 10.1 Contenedores y salud

- [ ] `docker compose ... ps` muestra PostgreSQL y backend activos.
- [ ] PostgreSQL muestra `healthy`.
- [ ] Backend responde `UP` en el puerto correcto.
- [ ] No hay reinicios repetidos.

### 10.2 Base de datos y Flyway

- [ ] La base del entorno existe.
- [ ] El usuario de base del entorno existe.
- [ ] Flyway tiene las migraciones `V1`-`V14` aplicadas.
- [ ] No hay migraciones fallidas.
- [ ] El volumen persistente esperado existe.

### 10.3 Aplicación

- [ ] El frontend carga en el puerto correspondiente.
- [ ] La comunicación frontend/backend funciona.
- [ ] El login funciona.
- [ ] Las pantallas principales cargan datos.
- [ ] DEV/TEST muestran los datos sintéticos esperados.
- [ ] PROD no contiene usuarios `@fluxis.local`.
- [ ] PROD no permite registro público.
- [ ] Los endpoints protegidos rechazan acceso sin sesión.

### 10.4 Persistencia

- [ ] Detener y volver a iniciar el backend conserva los datos.
- [ ] Recrear exclusivamente el backend no elimina el volumen.
- [ ] Los conteos de datos antes y después de una reconstrucción coinciden.

### 10.5 Separación de entornos

- [ ] DEV usa `fluxis_dev` y su volumen.
- [ ] TEST usa `fluxis_test` y su volumen.
- [ ] PROD usa `fluxis_prod` y su volumen.
- [ ] Ningún Compose apunta a la base de otro entorno.
- [ ] Las credenciales son distintas.
- [ ] TEST no puede usar `postgres_prod`.

## 11. Reglas de seguridad para instalaciones nuevas

- No generar ni cambiar contraseñas sin consentimiento.
- No inventar valores para secretos de producción.
- No reutilizar credenciales entre entornos.
- No publicar puertos de PostgreSQL sin autorización.
- No desactivar controles de seguridad para facilitar la instalación.
- No inicializar ni borrar bases existentes sin autorización expresa.
- No ejecutar operaciones destructivas asumiendo que una instalación está vacía.
- No exponer secretos en logs, documentación, terminales compartidas o respuestas.
- No versionar `.env`, backups, dumps ni archivos de credenciales.

Si falta un secreto o parámetro, debe proporcionarlo el propietario mediante un canal seguro y quedar registrado solo en el almacenamiento local/gestor de secretos correspondiente.

## 11.1 Resultado esperado

La documentación debe permitir al propietario:

- instalar Fluxis AI en un ordenador nuevo;
- crear DEV desde cero;
- crear TEST desde cero y de forma aislada;
- crear PROD desde cero con credenciales únicas y sin publicar PostgreSQL;
- levantar y detener cada entorno conservando sus datos;
- recuperar cada entorno ante incidencias sin recurrir a operaciones destructivas;
- distinguir instalación inicial, actualización y recuperación;
- reconocer cuándo una operación requiere consentimiento explícito.
## 12. Limitaciones y elementos no verificados

- La guía no fue ejecutada completamente en esta tarea; solo se revisaron los archivos y comandos contra el repositorio.
- No existe una matriz oficial de sistemas operativos en el proyecto.
- No se verificaron instalaciones macOS/Linux.
- No se verificó en esta tarea el contenido de `.env.prod` ni credenciales reales.
- El archivo `.env.prod.example` puede tener valores locales distintos de los defaults de Compose; el operador debe revisar la coherencia de puertos y URLs antes de arrancar.
- Las pruebas de Testcontainers requieren Docker activo y no usan las bases de los Compose.
- El frontend no está incluido en los archivos Compose; se ejecuta con Vite en el host o se publica como build estático.
- Los procedimientos de backup y restauración de PROD deben definirse según la política del propietario; no se inventa aquí un destino externo.

## 13. Referencias

- [Master Prompt](master-prompt.md)
- [Security](security.md)
- [Local Environments](environments.md)
- [Environment Validation](environment-validation.md)
- [Production Incidents and Recovery](production-incidents-and-recovery.md)
- [Data Model](data-model.md)
- [Architecture](architecture.md)
- [ADR-0001 Stack Versions](decisions/0001-stack-versions.md)
- [ADR-0005 Testing Strategy](decisions/0005-testing-strategy.md)
