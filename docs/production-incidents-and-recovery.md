# Production Incidents and Recovery

**Estado:** registro operativo versionado.
**Ámbito:** simulación local de PROD mediante `compose.prod.yaml` y procedimientos aplicables a una instalación PROD existente.
**Última revisión documental:** 2026-11-20.

Este documento registra incidentes reales observados durante la configuración, recuperación y puesta en funcionamiento de PROD. También define el procedimiento oficial de diagnóstico y recuperación.

## 1. Regla de oro: consentimiento obligatorio del propietario

> **NINGÚN AGENTE, ASISTENTE O AUTOMATIZACIÓN PUEDE MODIFICAR CONFIGURACIONES, CREDENCIALES O SECRETOS SIN MI CONSENTIMIENTO EXPLÍCITO Y PREVIO.**

Antes de ejecutar una operación protegida, el agente debe:

1. Explicar qué configuración pretende cambiar.
2. Explicar por qué lo considera necesario.
3. Identificar archivos, servicios, usuarios o datos afectados.
4. Describir riesgos y consecuencias.
5. Presentar el cambio propuesto.
6. Esperar autorización explícita del propietario.

No constituyen autorización:

- una petición genérica de solucionar un problema;
- una petición de revisar o diagnosticar;
- una autorización dada para otra operación;
- una suposición de que el cambio es necesario;
- el hecho de que el cambio parezca seguro o rutinario.

Sin consentimiento explícito solo se permite **inspeccionar, diagnosticar, explicar y proponer**.

La lista completa de operaciones protegidas está en [master-prompt.md](master-prompt.md) y [security.md](security.md).

## 2. Clasificación de evidencia

| Etiqueta | Significado |
|---|---|
| **Confirmado en repositorio** | Se puede comprobar en archivos versionados: Compose, Dockerfile, perfiles, código o migraciones. |
| **Confirmado operativamente** | Forma parte del incidente observado y fue reportado por el propietario. |
| **Hipótesis** | Explicación plausible, pero no demostrada con evidencia conservada. |
| **No verificado** | Requiere una comprobación posterior en el entorno real. |

Los secretos, contraseñas, tokens y valores reales de credenciales **no se documentan ni se versionan**.

## 3. Contrato operativo actual de PROD

| Elemento | Valor actual |
|---|---|
| Proyecto Compose | `fluxis-env-prod` |
| Archivo Compose | `compose.prod.yaml` |
| Archivo de variables | `.env.prod` (local, ignorado por Git) |
| Servicio PostgreSQL | `postgres_prod` |
| Contenedor PostgreSQL | `postgres-prod` |
| Servicio backend | `backend_prod` |
| Contenedor backend | `env-prod` |
| Imagen backend | `fluxis-ai-backend:env-prod` |
| Dockerfile | `backend/Dockerfile` |
| Contexto de build | `./backend` |
| JAR dentro del contenedor | `/app/app.jar` |
| Perfil Spring Boot | `prod` |
| Puerto backend en host | `8085` por defecto (`PROD_SERVER_PORT`) |
| Frontend local | `http://localhost:5175` por defecto |
| Volumen PostgreSQL | `fluxis-ai_postgres_prod_data` |
| Puerto PostgreSQL publicado | Ninguno |
| Migraciones actuales | `V1` a `V14` |

El volumen tiene un nombre explícito, por lo que no depende únicamente del nombre del proyecto Compose. Aun así, todas las operaciones oficiales deben usar `--project-name fluxis-env-prod` para evitar ambigüedad.

## 4. Incidente 1: fallo de autenticación PostgreSQL en PROD

### 4.1 Síntoma

El backend PROD no pudo autenticarse contra PostgreSQL. El error observado fue equivalente a:

```text
FATAL: password authentication failed for user "fluxis_prod"
SQLSTATE: 28P01
```

**Evidencia:** confirmado operativamente.

### 4.2 Diferencia entre `.env.prod` y el rol PostgreSQL

Un valor `POSTGRES_PROD_PASSWORD` en `.env.prod` es una entrada de configuración usada por Docker Compose. No es una fuente de verdad permanente sobre la contraseña del rol.

- En la primera inicialización de un volumen PostgreSQL vacío, la imagen oficial usa `POSTGRES_PASSWORD` para crear el rol.
- Una vez inicializado el volumen, la credencial del rol queda almacenada dentro de los archivos de datos de PostgreSQL.
- Cambiar `.env.prod` después no cambia automáticamente la contraseña almacenada en el rol.
- El backend debe usar exactamente la credencial vigente del rol, salvo que el propietario autorice una rotación explícita.

Por eso puede ocurrir que:

1. `.env.prod` contenga una contraseña nueva;
2. el volumen conserve la contraseña anterior;
3. PostgreSQL rechace la conexión del backend con SQLSTATE `28P01`.

### 4.3 Persistencia de credenciales en el volumen

El volumen `fluxis-ai_postgres_prod_data` contiene el directorio de datos de PostgreSQL. Allí persisten, entre otros:

- bases de datos y tablas;
- usuarios y roles;
- hashes de credenciales de roles;
- historial de Flyway;
- datos de la aplicación.

Eliminar el volumen elimina todos esos elementos. **Nunca se debe eliminar el volumen para resolver un fallo de autenticación.**

### 4.4 Procedimiento de diagnóstico de solo lectura

Definir primero las variables comunes:

```powershell
$composeRoot = 'C:\Users\pudre\Documents\GitHub\Fluxis-AI'
Set-Location $composeRoot
$prodEnvFile = Join-Path $composeRoot '.env.prod'
$prodComposeFile = Join-Path $composeRoot 'compose.prod.yaml'
```

Comprobar estado de servicios:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile ps
```

Leer logs del backend y PostgreSQL:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 backend_prod
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 postgres_prod
```

Inspeccionar únicamente valores no secretos:

```powershell
docker inspect postgres-prod --format '{{range .Config.Env}}{{println .}}{{end}}' |
  Select-String '^POSTGRES_(DB|USER)='
docker inspect env-prod --format '{{range .Config.Env}}{{println .}}{{end}}' |
  Select-String '^SPRING_DATASOURCE_(URL|USERNAME)='
```

No imprimir `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` ni secretos equivalentes.
Comprobar autenticación con la credencial disponible dentro del contenedor, sin imprimirla:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT current_user, current_database();"'
```

Comprobar salud del backend, que incluye la conexión del datasource:

```powershell
Invoke-RestMethod http://localhost:8085/actuator/health
```

Resultado esperado cuando la autenticación está resuelta:

```json
{ "status": "UP" }
```

**Nota:** si el backend está caído, el health check fallará aunque PostgreSQL esté correcto. Los logs del backend son la evidencia principal para distinguir un fallo de credenciales.

### 4.5 Opciones de recuperación no destructivas

La elección entre estas opciones es una modificación protegida y requiere autorización explícita del propietario.

#### Opción A: conservar la credencial existente del rol

Consiste en restaurar en `.env.prod` la contraseña que ya está almacenada en el volumen y recrear únicamente el backend.

```powershell
# [PROTEGIDO: requiere autorización] Editar .env.prod para alinear la credencial.
# El agente no debe editar el archivo ni mostrar su contenido sensible.

# [REINICIO DE SERVICIO] Recrear solo el backend.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod
```

Ventaja: no modifica el rol ni los datos de PostgreSQL.

#### Opción B: rotar la contraseña del rol con autorización

```powershell
# [PROTEGIDO: requiere autorización explícita para cambiar credenciales]
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec postgres_prod psql -U fluxis_prod -d fluxis_prod
```

Dentro de `psql`, usar el comando interactivo:

```text
\password fluxis_prod
```

Después, con autorización, actualizar `.env.prod` y recrear únicamente el backend:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod
```

Ventaja: permite rotar una credencial comprometida.
Riesgo: cualquier otro cliente que use la credencial anterior dejará de autenticarse.

### 4.6 Solución aplicada y registro de evidencia

El incidente se abordó sin eliminar el volumen de PostgreSQL. El repositorio no conserva el comando exacto ni la credencial utilizada, por diseño de seguridad. Por tanto:

- **Confirmado:** no se debe resolver este incidente borrando datos.
- **Confirmado:** la credencial efectiva debe ser única para PROD y no reutilizarse desde DEV o TEST.
- **No verificado en repositorio:** cuál de las dos opciones no destructivas se ejecutó finalmente.
- **No verificado en repositorio:** la hora exacta y el operador de la corrección.

Si el propietario necesita conservar un registro exacto, debe anotarlo en un gestor de secretos o bitácora privada: fecha, opción aplicada, servicio recreado y verificación posterior. Nunca debe anotarse la contraseña.

### 4.7 Precauciones

- No ejecutar `docker compose down -v`.
- No eliminar `fluxis-ai_postgres_prod_data`.
- No ejecutar `docker volume rm`.- No cambiar contraseñas para probar una hipótesis.
- No reutilizar credenciales de DEV o TEST.- No imprimir secretos en logs, terminales compartidas, documentación o respuestas.
- No recrear PostgreSQL si el problema es solo la credencial usada por el backend.

## 5. Incidente 2: persistencia de datos y configuración Docker Compose

### 5.1 Contenedores frente a volúmenes

| Operación | Qué elimina | Pierde datos PostgreSQL |
|---|---|---|
| `docker compose stop` | Detiene procesos; conserva contenedores, redes y volúmenes. | No |
| `docker compose down` | Elimina contenedores y red; conserva volúmenes nombrados. | Normalmente no, si no se usa `-v`. |
| `docker compose down -v` | Elimina contenedores, red y volúmenes declarados. | **Sí. Es destructivo.** |
| `docker volume rm fluxis-ai_postgres_prod_data` | Elimina el volumen nombrado. | **Sí. Es destructivo.** |
| Recrear solo `backend_prod` | Sustituye el contenedor del backend. | No afecta al volumen. |
| Recrear solo `postgres_prod` | Sustituye PostgreSQL usando el mismo volumen. | No, si se conserva el volumen y coincide la credencial. |

### 5.2 Importancia del volumen `fluxis-ai_postgres_prod_data`

`compose.prod.yaml` declara:

```yaml
volumes:
  postgres_prod_data:
    name: fluxis-ai_postgres_prod_data
```

Ese nombre explícito identifica el volumen persistente real. Es la frontera entre una operación recuperable y una pérdida de datos.

```powershell
# [SOLO LECTURA]
docker volume inspect fluxis-ai_postgres_prod_data
```

### 5.3 Nombre de proyecto anterior `fluxis-ai` y actual `fluxis-env-prod`

Advertencia operativa: en configuraciones anteriores o comandos antiguos se utilizó el nombre de proyecto `fluxis-ai`. El Compose actual declara:

```yaml
name: fluxis-env-prod
```

Implicaciones:

- Docker Compose identifica contenedores, redes y recursos por proyecto.
- Un comando sin el `--project-name` correcto puede inspeccionar o gestionar otro conjunto de contenedores.
- Aunque el volumen tiene nombre explícito y debería reutilizarse, no se debe asumir que cualquier proyecto Compose apunta al mismo entorno.
- Usar siempre `--project-name fluxis-env-prod` en los procedimientos oficiales.
- No usar limpiezas globales como `docker system prune --volumes`.

### 5.4 Servicios actuales

`postgres_prod`:

- imagen `postgres:18`;
- contenedor `postgres-prod`;
- base y usuario configurables por `.env.prod`;
- volumen `fluxis-ai_postgres_prod_data`;
- sin puerto publicado al host;
- healthcheck con `pg_isready`.

`backend_prod`:

- imagen `fluxis-ai-backend:env-prod`;
- build desde `./backend` y `Dockerfile`;
- contenedor `env-prod`;
- perfil `prod`;
- depende de `postgres_prod` saludable;
- puerto host por defecto `8085`;
- healthcheck contra `/actuator/health`.

### 5.5 Riesgos de `docker compose down -v`

`down -v` puede eliminar el volumen de PostgreSQL y, con él:

- usuarios y datos;
- credenciales de roles;
- historial Flyway;
- capacidad de recuperar el estado sin un backup previo.

**Prohibición:** nunca ejecutar `docker compose down -v` en PROD.

### 5.6 Reconstrucción exclusiva del backend

Secuencia propuesta, sujeta a autorización explícita porque reconstruye y recrea un servicio PROD:

```powershell
# [SOLO LECTURA] Validar el modelo Compose sin iniciar contenedores.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile config --quiet

# [RECONSTRUYE IMAGEN] Construir solo backend_prod.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile build backend_prod

# [RECREA SERVICIO] Recrear solo backend_prod, sin tocar PostgreSQL.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod
```

Comprobaciones posteriores:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile ps
Invoke-RestMethod http://localhost:8085/actuator/health
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 backend_prod
```

## 6. Incidente 3: backend PROD desactualizado

### 6.1 Síntoma

El contenedor `env-prod` ejecutaba una imagen/JAR anterior. La aplicación respondía `404` en endpoints que sí existen en el código actual:

- `/api/job-applications`
- `/api/dashboard`

**Evidencia:** confirmado operativamente.

### 6.2 Causa

El JAR contenido en el contenedor no incluía:

- `JobApplicationController`;
- `DashboardController`;
- las migraciones Flyway `V3` a `V14`.

La causa inmediata fue un contenedor/imagen desactualizado respecto del código y del Dockerfile actuales. La causa de por qué no se reconstruyó en el momento no está documentada en el repositorio y queda como **no verificada**.

Un endpoint inexistente en el JAR produce `404`. Un endpoint existente pero protegido sin sesión normalmente produce `401`; por eso no debe confundirse un `404` con un problema de autenticación.

### 6.3 Inspección del JAR dentro del contenedor

Estas órdenes son de solo lectura respecto del contenedor. Copian el JAR a un directorio temporal local para inspección:

```powershell
# [SOLO LECTURA] Copiar el JAR del contenedor a un archivo temporal.
New-Item -ItemType Directory -Force .\tmp | Out-Null
docker cp env-prod:/app/app.jar .\tmp\app-prod-inspect.jar

# [SOLO LECTURA] Comprobar controladores.
jar tf .\tmp\app-prod-inspect.jar | Select-String 'JobApplicationController|DashboardController'

# [SOLO LECTURA] Listar migraciones empaquetadas.
jar tf .\tmp\app-prod-inspect.jar | Select-String 'BOOT-INF/classes/db/migration/V[0-9]+__'
```

Resultado esperado después de corregir:

- aparecen ambas clases de controlador;
- aparecen las migraciones `V1` a `V14`;
- el JAR inspeccionado corresponde al contenedor que se está ejecutando.

```powershell
# [SOLO LECTURA] Eliminar el archivo temporal de inspección.
Remove-Item .\tmp\app-prod-inspect.jar
```

### 6.4 Identificación de imagen, Dockerfile y contexto

```powershell
# [SOLO LECTURA]
docker inspect env-prod --format '{{.Config.Image}} {{.Image}}'
docker image inspect fluxis-ai-backend:env-prod --format '{{.Id}} {{.Created}}'
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile config --quiet
```

Datos confirmados en repositorio:

- imagen: `fluxis-ai-backend:env-prod`;
- contexto: `./backend`;
- Dockerfile: `Dockerfile`;
- JAR final dentro del contenedor: `/app/app.jar`;
- build multi-stage: Maven + JDK 25 para compilar; JRE 25 para ejecutar.

### 6.5 Reconstrucción exclusiva del backend

Operación protegida: requiere autorización explícita porque reconstruye y recrea un servicio PROD. Antes de ejecutarla, confirmar que no se eliminará ningún volumen.

```powershell
# [RECONSTRUYE IMAGEN] Solo backend_prod.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile build --no-cache backend_prod

# [RECREA SERVICIO] Solo backend_prod, sin dependencias.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod
```

`--no-deps` evita recrear `postgres_prod`. `--force-recreate` afecta solo al servicio indicado.

### 6.6 Verificación posterior del nuevo JAR

```powershell
docker cp env-prod:/app/app.jar .\tmp\app-prod-inspect.jar
jar tf .\tmp\app-prod-inspect.jar | Select-String 'JobApplicationController|DashboardController'
jar tf .\tmp\app-prod-inspect.jar | Select-String 'BOOT-INF/classes/db/migration/V[0-9]+__'
Invoke-RestMethod http://localhost:8085/actuator/health
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 backend_prod
```

Comprobación funcional autenticada: iniciar sesión en `http://localhost:5175` y confirmar que `/api/job-applications` y `/api/dashboard` responden sin `404`.

## 7. Incidente 4: validación de Flyway después de la actualización

### 7.1 Evidencia observada

Después de actualizar el backend:

- PostgreSQL ya estaba en la versión **14**;
- Flyway validó correctamente las **14 migraciones**;
- el arranque mostró el mensaje:

```text
Schema "public" is up to date. No migration necessary.
```

**Evidencia:** confirmado operativamente.

### 7.2 Validar no es migrar

| Acción | Qué hace | Modifica el esquema |
|---|---|---|
| `validate` | Comprueba historial, checksums y estado de migraciones aplicadas. | No |
| `migrate` | Aplica migraciones pendientes. | Sí, si hay pendientes |
| Arranque de Spring Boot con Flyway | Ejecuta la lógica configurada; si no hay pendientes, valida y continúa. | Solo si hay pendientes |

Un mensaje "Schema is up to date" significa que no había migraciones nuevas que aplicar. No significa que se haya ejecutado una migración nueva.

### 7.3 Comprobación de solo lectura del historial

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Resumen:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) AS applied, count(*) FILTER (WHERE success) AS successful, max(version) AS max_version FROM flyway_schema_history;"'
```

Resultado esperado para el estado documentado:

- `max_version = 14`;
- 14 registros aplicados;
- todos con `success = true`;
- ninguna migración pendiente.

### 7.4 Comprobación de arranque sin modificar el esquema

```powershell
# [REINICIO DE SERVICIO] Requiere autorización si existe cualquier migración pendiente.
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod

# [SOLO LECTURA]
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 backend_prod
Invoke-RestMethod http://localhost:8085/actuator/health
```

Criterios:

- no aparecen errores de Flyway;
- no se aplican migraciones inesperadas;
- el backend queda `UP`;
- el JAR del contenedor incluye las migraciones esperadas.

## 8. Procedimiento oficial de recuperación de PROD

Este procedimiento es para una instalación PROD existente. **No debe usarse como inicialización desde cero.**

### 8.1 Paso 1: diagnóstico inicial y control de cambios

1. Confirmar la autorización del propietario para cualquier operación protegida.
2. Registrar fecha, síntoma, endpoint afectado y último cambio conocido.
3. No modificar archivos, secretos, contenedores ni datos durante el diagnóstico.
4. Definir una ventana de mantenimiento si se va a recrear el backend.

```powershell
Set-Location 'C:\Users\pudre\Documents\GitHub\Fluxis-AI'
$prodEnvFile = Join-Path (Get-Location) '.env.prod'
$prodComposeFile = Join-Path (Get-Location) 'compose.prod.yaml'
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile config --quiet
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile ps
```

### 8.2 Paso 2: contenedores, imágenes y logs

```powershell
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}'
docker inspect env-prod --format '{{.Config.Image}} {{.Image}} {{.State.Status}}'
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 postgres_prod
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile logs --tail=200 backend_prod
```

Buscar en los logs: `28P01`, `password authentication failed`, `FATAL`, `Flyway`, `migration`, `schema`, excepciones de arranque, `404`, `401`, `403`, reinicios o crashes. No imprimir variables sensibles.

### 8.3 Paso 3: verificación de credenciales sin exponerlas

```powershell
Invoke-RestMethod http://localhost:8085/actuator/health
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1;"'
```

Si el backend muestra `28P01`:

1. no borrar el volumen;
2. comparar la configuración sin imprimir contraseñas;
3. elegir una opción no destructiva con autorización;
4. recrear solo el backend.

### 8.4 Paso 4: inspección de migraciones Flyway

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Verificar:

- todas las filas tienen `success = true`;
- no hay versiones inesperadas;
- no hay migraciones pendientes;
- el estado máximo coincide con la versión esperada.

No ejecutar migraciones manualmente ni reiniciar el backend si hay pendientes sin autorización expresa.

### 8.5 Paso 5: salud del backend y PostgreSQL

```powershell
Invoke-RestMethod http://localhost:8085/actuator/health
docker inspect postgres-prod --format '{{.State.Health.Status}}'
docker inspect env-prod --format '{{.State.Health.Status}}'
```

Criterios:

- health check del backend en `UP`;
- health check de PostgreSQL en `healthy`;
- backend sin reinicios repetidos;
- logs sin errores de autenticación, Flyway o arranque.

### 8.6 Paso 6: reconstrucción exclusiva del backend cuando esté justificada

Solo si la evidencia demuestra que el JAR/imagen está desactualizado, y con autorización explícita:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile build --no-cache backend_prod
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile up -d --no-deps --force-recreate backend_prod
```

No usar `down`, no eliminar el volumen, no recrear PostgreSQL y no ejecutar migraciones manualmente.

### 8.7 Paso 7: verificación de integridad de datos

Antes y después de una reconstrucción del backend, registrar conteos de solo lectura:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT (SELECT count(*) FROM users) AS users, (SELECT count(*) FROM job_opportunities) AS job_opportunities, (SELECT count(*) FROM job_applications) AS job_applications, (SELECT count(*) FROM tasks) AS tasks;"'
```

Comprobar que no haya usuarios sintéticos locales en PROD:

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) AS synthetic_local_users FROM users WHERE email LIKE ''%@fluxis.local'';"'
```

Resultado esperado en PROD: `0`. Si no es cero, detenerse e investigar; no borrar datos sin autorización.

```powershell
docker compose --project-name fluxis-env-prod --env-file $prodEnvFile --file $prodComposeFile exec -T postgres_prod sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) AS failed_migrations FROM flyway_schema_history WHERE success = false;"'
```

Resultado esperado: `0`.

Si se necesita un backup antes de una operación autorizada, debe definirse una ruta segura y protegerse el archivo resultante. Los backups de PROD contienen datos sensibles y no deben versionarse ni compartirse.

### 8.8 Paso 8: pruebas funcionales posteriores

Comprobaciones mínimas:

1. `GET /actuator/health` devuelve `UP`.
2. El frontend PROD carga en `http://localhost:5175`.
3. El login con la cuenta PROD funciona.
4. `/api/dashboard` responde sin `404`.
5. `/api/job-applications` responde sin `404`.
6. Las pantallas principales cargan datos.
7. Un reinicio controlado del backend conserva los datos.
8. Los logs no muestran errores nuevos.
9. Los intentos de registro público están deshabilitados.
10. Los endpoints protegidos sin sesión no exponen datos.

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod -Uri http://localhost:8085/api/auth/csrf -WebSession $session
$headers = @{}
$headers[$csrf.headerName] = $csrf.token

$body = @{
  email = 'REPLACE_WITH_PROD_ACCOUNT_EMAIL'
  password = 'REPLACE_WITH_PROD_ACCOUNT_PASSWORD'
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8085/api/auth/login -WebSession $session -Headers $headers -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri http://localhost:8085/api/auth/me -WebSession $session
Invoke-RestMethod -Uri http://localhost:8085/api/dashboard -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8085/api/job-applications?page=0&size=20' -WebSession $session
```

No guardar la contraseña en el historial de PowerShell. Para pruebas reales, usar el prompt interactivo o una variable de entorno segura y nunca documentar el valor.

## 9. Matriz de clasificación de comandos

| Clasificación | Ejemplos | Riesgo | Autorización |
|---|---|---|---|
| Solo lectura | `ps`, `logs`, `inspect`, `config --quiet`, `jar tf`, `docker cp` de salida, consultas `SELECT` | Bajo | No, salvo que el comando pueda revelar secretos |
| Reinicio de servicio | `restart backend_prod`, `up -d --no-deps --force-recreate backend_prod` | Medio: interrupción temporal | Sí, en PROD |
| Reconstrucción de imagen | `docker compose build backend_prod` | Medio: consume recursos y cambia la imagen local | Sí, en PROD |
| Modifica configuración | editar `.env.prod`, variables, puertos, URLs, cookies o perfiles | Alto | Sí, explícita |
| Modifica datos o credenciales | `ALTER ROLE`, `\password`, DML, migraciones | Alto | Sí, explícita |
| Destructivo | `down -v`, `docker volume rm`, borrar bases o volúmenes | Crítico | Prohibido sin autorización excepcional y backup verificado |

## 10. Prohibiciones específicas de PROD

- Nunca ejecutar `docker compose down -v` en PROD.
- Nunca eliminar ni recrear el volumen de PostgreSQL para resolver problemas del backend.
- Nunca ejecutar migraciones manualmente sin autorización expresa.
- Nunca modificar datos de producción para facilitar pruebas.
- Nunca cambiar contraseñas para intentar resolver errores sin autorización.
- Nunca reconstruir o desplegar servicios PROD sin autorización expresa.
- Nunca utilizar credenciales de DEV o TEST en PROD.
- Nunca exponer secretos en logs, documentación, terminales compartidos o respuestas.
- Nunca asumir que una instalación está vacía antes de ejecutar una operación destructiva.

## 11. Checklist de cierre de un incidente

- [ ] Síntoma reproducido o documentado.
- [ ] Logs y estado revisados sin exponer secretos.
- [ ] Causa inmediata identificada o marcada como hipótesis.
- [ ] Autorización obtenida para la operación correctiva.
- [ ] Volumen y datos preservados.
- [ ] Migraciones y checksum de Flyway verificados.
- [ ] Backend saludable.
- [ ] Endpoints afectados verificados.
- [ ] Datos esperados preservados.
- [ ] Sin datos sintéticos en PROD.
- [ ] Evidencia registrada sin secretos.
- [ ] Prevención o seguimiento documentado.

## 12. Referencias

- [Master Prompt](master-prompt.md)
- [Security](security.md)
- [Local Environments](environments.md)
- [Environment Validation](environment-validation.md)
- [Installation from Scratch](installation-from-scratch.md)
- [Data Model](data-model.md)
