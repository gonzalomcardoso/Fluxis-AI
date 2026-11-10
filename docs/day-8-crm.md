# Día 8 Empresas y contactos

El módulo CRM del día 8 permite gestionar empresas y contactos del usuario autenticado desde la API y la interfaz web.

## Alcance

- Empresas con nombre, sitio web, sector y notas.
- Contactos con nombre, apellidos, correo, cargo, origen, notas y empresa opcional.
- CRUD completo: crear, listar, consultar, actualizar y eliminar.
- Aislamiento por usuario mediante `CurrentUser` en todos los servicios y repositorios.
- Detección de duplicados: nombre de empresa sin distinguir mayúsculas/minúsculas y correo de contacto sin distinguir mayúsculas/minúsculas.
- La relación contacto-empresa usa `ON DELETE SET NULL`, por lo que eliminar una empresa no elimina sus contactos.

## API

| Recurso | Endpoints |
|---|---|
| Empresas | `GET/POST /api/companies`, `GET/PUT/DELETE /api/companies/{id}` |
| Contactos | `GET/POST /api/contacts`, `GET/PUT/DELETE /api/contacts/{id}` |

Las operaciones que modifican datos requieren la sesión autenticada y el token CSRF gestionado por el cliente. Los conflictos de duplicado responden con HTTP 409 y los recursos de otro usuario se comportan como no encontrados.

## Interfaz

La pantalla está disponible en `/crm`. Incluye formularios de alta y edición, listados de empresas y contactos, asociación opcional de contactos con empresas y acciones de eliminación.

## Verificación

Desde `backend/`:

```powershell
.\mvnw.cmd test -q
```

Desde `frontend/`:

```powershell
npm.cmd run build
```

La migración [V3__create_companies_and_contacts.sql](../backend/src/main/resources/db/migration/V3__create_companies_and_contacts.sql) crea tablas, claves foráneas, índices de consulta e índices únicos para los duplicados.
