# Día 10 Clientes y servicios profesionales

La migración correctiva [V6__align_service_currency_type.sql](../backend/src/main/resources/db/migration/V6__align_service_currency_type.sql) alinea `services.currency` con el tipo `VARCHAR(3)` esperado por Hibernate, sin modificar una migración ya aplicada.

El día 10 añade la gestión de clientes y del catálogo de servicios profesionales.

## Alcance

- Clientes con empresa y contacto opcionales, estado, origen y notas.
- Servicios profesionales con nombre, categoría, descripción, tarifa, moneda, disponibilidad y estado.
- Modelos de precio: por hora, precio fijo y recurrente.
- Tarifas almacenadas como `NUMERIC(19,4)` y validadas con `BigDecimal`.
- Validación de que las empresas y contactos relacionados pertenecen al usuario actual.
- CRUD completo desde la API y la pantalla `/clientes-servicios`.

## API

```text
GET/POST       /api/clients
GET/PUT/DELETE /api/clients/{id}
GET/POST       /api/services
GET/PUT/DELETE /api/services/{id}
```

La migración [V5__create_clients_and_services.sql](../backend/src/main/resources/db/migration/V5__create_clients_and_services.sql) crea las tablas, restricciones e índices. Las monedas se validan como códigos de tres letras mayúsculas y las tarifas deben ser mayores que cero.
