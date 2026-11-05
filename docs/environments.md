# Ambientes de desarrollo y pruebas

Las ramas Git (`dev`, `test`) no seleccionan perfiles de ejecución. Spring usa el perfil `dev` por defecto; para pruebas se selecciona `test`.

## Comparación

| Propiedad | Desarrollo (`dev`) | Pruebas (`test`) |
|---|---|---|
| Base manual Compose | `fluxis_dev` | `fluxis_test` |
| PostgreSQL | Servicio Compose `postgres`, PostgreSQL 18 | Servicio Compose `postgres_test` o PostgreSQL efímero de Testcontainers |
| API | `backend-dev` | `backend-test` (perfil Compose `test`) |
| Imagen del backend (prueba actual) | `fluxis-ai-backend:feat-d6-a` | `fluxis-ai-backend:dev` |
| Frontend local | Vite en `http://localhost:5173` → API `8080` | Vite en `http://localhost:5174` → API `8081` |
| Puerto PostgreSQL local | `5432` | `5433` |
| Volumen Compose | `postgres_dev_data` | `postgres_test_data` |
| Perfil Spring | `dev` (predeterminado y explícito en Compose) | `test` |
| Flyway | Activo | Activo; en tests automatizados aplica desde cero |
| Hibernate | `validate` | `validate` |
| Datos sintéticos | Seeder idempotente: dos usuarios con dos oportunidades cada uno | Seeder opcional `test-data`: 120 oportunidades sintéticas para pruebas manuales; identidad sintética para pruebas de API |
| OpenAPI | Se habilitará al integrar la librería en el Paso D | Se habilitará al integrar la librería en el Paso D |
| Servicios externos | Ninguno | Ninguno |

El archivo de Compose se conserva en la raíz (`compose.yaml`) para no romper los comandos existentes. `.env.example` contiene valores ficticios para pruebas manuales; `postgres-test` reutiliza `POSTGRES_PASSWORD`, mientras que base de datos, usuario, puerto y volumen permanecen separados.

## Desarrollo local

Desde la raíz, después de copiar `.env.example` a `.env` y establecer `POSTGRES_PASSWORD`:

```powershell
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Compose fuerza `SPRING_PROFILES_ACTIVE=dev`. PostgreSQL escucha solo en `127.0.0.1:5432` y usa `fluxis_dev`. Para ejecutar el backend desde Maven en vez de Docker, deja PostgreSQL arriba y en `backend/` ejecuta:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

El perfil `dev` obtiene la contraseña de `POSTGRES_PASSWORD`. El CORS de `/api/**` permite únicamente el origen `APP_FRONTEND_URL` (por defecto `http://localhost:5173`). Los datos de demo son sintéticos y usan dominios reservados `.test`.

En otra terminal, desde `frontend/`, inicia el frontend de desarrollo:

```powershell
npm run dev
```

Abre `http://localhost:5173`. El proxy de Vite dirige `/api` al backend dev en el puerto `8080`.

## Pruebas automatizadas

Desde `backend/`, con Docker activo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd verify
```

Las pruebas de integración usan Testcontainers y un PostgreSQL 18 efímero; no se conectan a `fluxis_dev` ni al servicio Compose opcional. Flyway ejecuta la migración desde cero en ese contenedor.

## Aplicaciones separadas para pruebas manuales

Desde la raíz, inicia el backend y PostgreSQL aislados del ambiente de pruebas:

```powershell
docker compose --profile test up -d --build
docker compose --profile test ps
Invoke-RestMethod http://localhost:8081/actuator/health
```

Esto inicia `postgres-test` en `5433` y `backend-test` en `8081`, conectados a `fluxis_test` mediante una red interna de Compose. La base tiene usuario y volumen propios, y reutiliza la contraseña local `POSTGRES_PASSWORD` de dev; las credenciales permanecen en `.env`, que no se debe compartir. El perfil `test-data` carga 120 oportunidades sintéticas en inglés para el usuario de pruebas. El seeder es idempotente y no se activa en las pruebas automatizadas de Testcontainers.

Para comparar la rama de trabajo y `dev`, los JAR están guardados localmente como `backend/fluxis-ai-backend-feat-d6-a.jar` y `backend/fluxis-ai-backend-dev.jar` (ignorados por Git). Sus imágenes Docker tienen etiquetas correspondientes: `fluxis-ai-backend:feat-d6-a` y `fluxis-ai-backend:dev`. En esta configuración, el ambiente Docker dev ejecuta `feat/d6-a`, mientras que el ambiente Docker test ejecuta el JAR de `dev`; cada uno mantiene su base de datos aislada. El JAR de `dev` se ejecuta con el perfil Spring `dev` también dentro del ambiente test porque esa versión no contiene un proveedor de identidad para el perfil `test`; sus propiedades de conexión apuntan explícitamente a `postgres-test`. La base test conserva sus 120 oportunidades asociadas a la identidad sintética de integración; como la identidad activa del JAR de dev es diferente, el listado API de ese ambiente puede aparecer vacío aunque haya registros en la base. Para reconstruir una etiqueta, sitúate en la rama deseada y construye explícitamente, evitando que Compose mezcle ramas.

Desde `backend/`, para actualizar el artefacto de dev:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-dev.jar
docker build --tag fluxis-ai-backend:dev .
```

En la rama `feat/d6-a`, usa los nombres `fluxis-ai-backend-feat-d6-a.jar` y `fluxis-ai-backend:feat-d6-a`. Tras reconstruir una etiqueta, desde la raíz recrea el servicio asignado:

```powershell
docker compose up -d backend
docker compose --profile test up -d backend_test
```

### Actualizar test después de integrar un feature en dev

Cuando un PR `feat/*` ya se haya integrado en `dev`, el usuario cambia a la rama `dev`. Desde `backend/`, compila el JAR y construye la imagen `dev` con el código integrado:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item .\target\fluxis-ai-backend-0.1.0-SNAPSHOT.jar .\fluxis-ai-backend-dev.jar -Force
docker build --tag fluxis-ai-backend:dev .
```

Desde la raíz, actualiza únicamente el backend del ambiente test y verifica su salud:

```powershell
docker compose --profile test up -d backend_test
Invoke-RestMethod http://localhost:8081/actuator/health
```

Esto conserva `postgres-test` y sus datos. No basta con abrir el PR: `dev` debe contener el merge para que el artefacto incluya el feature integrado. Los comandos Git y los cambios de rama los ejecuta el usuario.

En otra terminal, desde `frontend/`, inicia el segundo frontend:

```powershell
npm run dev:test
```

Abre `http://localhost:5174`. El modo Vite `test` dirige `/api` al backend test en `8081`; el frontend dev en `5173` sigue apuntando a `8080`. Así puedes tener ambas aplicaciones abiertas a la vez sin compartir base de datos.

Detén solo los servicios Docker de test sin borrar sus datos:

```powershell
docker compose --profile test stop backend_test postgres_test
```

Para recrear únicamente la base manual de test, primero detenla y elimina **solo** el volumen `postgres_test_data`. El nombre completo suele llevar el prefijo del proyecto Compose; compruébalo antes de borrarlo:

```powershell
docker volume ls --filter label=com.docker.compose.volume=postgres_test_data
docker volume rm <nombre-exacto-del-volumen-de-test>
docker compose --profile test up -d postgres_test
```

No uses `docker compose down -v` para resetear solo test: podría eliminar también el volumen de desarrollo. Para resetear dev, identifica de forma análoga y elimina únicamente el volumen `postgres_dev_data`; eso borra los datos locales de desarrollo.

## Verificar que las bases no se comparten

```powershell
docker compose ps
docker compose --profile test ps
docker compose exec postgres psql -U fluxis_dev -d fluxis_dev -c "SELECT current_database();"
docker compose --profile test exec postgres_test psql -U fluxis_test -d fluxis_test -c "SELECT current_database();"
```

Los servicios, puertos y volúmenes son independientes. No se copian datos entre ambos ambientes.
