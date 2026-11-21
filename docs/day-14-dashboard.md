# Día 14 — Dashboard v1

## Alcance

El dashboard agrega datos reales de clientes, servicios, propuestas, suscripciones y tareas mediante `GET /api/dashboard`.

Las métricas monetarias se mantienen separadas por moneda. El margen estimado se calcula por cada moneda como:

`propuestas aceptadas - gasto mensual de suscripciones activas`

No se hace ninguna conversión ni suma entre monedas.

## Métricas

- Clientes totales, potenciales y activos.
- Servicios totales y activos.
- Propuestas totales, por estado e importe aceptado por moneda.
- Suscripciones activas, gasto mensual/anual por moneda y renovaciones de los próximos 30 días.
- Tareas pendientes, en curso, completadas y vencidas.
- Margen estimado por moneda.

## Verificación

- Pruebas unitarias de conversión de suscripciones y separación del margen por moneda.
- Build frontend validado con TypeScript y Vite.
