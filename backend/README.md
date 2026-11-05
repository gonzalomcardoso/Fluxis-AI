# Fluxis AI Backend

## Requisitos

- JDK 25.
- Docker disponible para las pruebas de integración con Testcontainers.
- PostgreSQL local solo si se arranca la aplicación fuera de Docker.

## Pruebas

Desde `backend/`, en PowerShell:

```powershell
.\mvnw.cmd test
```

Ejecuta las pruebas unitarias del servicio y las pruebas web de `@WebMvcTest` sin iniciar PostgreSQL.

```powershell
.\mvnw.cmd verify
```

Ejecuta también las pruebas de integración `*IT` mediante Maven Failsafe. Estas usan un contenedor PostgreSQL 18 compartido durante la ejecución; no usan H2 ni requieren un servicio PostgreSQL adicional. La primera ejecución descarga las imágenes y dependencias necesarias.

Para ejecutar explícitamente las pruebas bajo el perfil `test`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd verify
```

## Convención de pruebas

- `*Test`: unitarias o de capa web; las ejecuta Maven Surefire durante `test`.
- `*IT`: integración con PostgreSQL real; la ejecuta Maven Failsafe durante `verify`.

Las migraciones Flyway se aplican al arrancar cada contexto de integración sobre el contenedor de pruebas.
