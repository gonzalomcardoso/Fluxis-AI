# Día 9 Candidaturas

El día 9 añade el seguimiento estructurado de candidaturas a partir de las oportunidades laborales existentes.

## Alcance

- Una oportunidad laboral puede convertirse en una candidatura una sola vez por usuario.
- Estados: pendiente, interesante, preparando candidatura, candidatura enviada, entrevista, oferta recibida, rechazada y descartada.
- Notas y fecha de candidatura.
- Entrevistas con fecha, tipo y notas.
- Seguimientos con fecha, descripción y estado de finalización.
- Aislamiento por usuario en candidaturas, entrevistas y seguimientos.

## API

```text
GET/POST       /api/job-applications
GET/PUT/DELETE /api/job-applications/{id}
GET/POST       /api/job-applications/{id}/interviews
PUT/DELETE     /api/job-applications/{id}/interviews/{interviewId}
GET/POST       /api/job-applications/{id}/follow-ups
PUT/DELETE     /api/job-applications/{id}/follow-ups/{followUpId}
```

La interfaz está disponible en `/candidaturas`. La migración [V4__create_job_applications_and_follow_ups.sql](../backend/src/main/resources/db/migration/V4__create_job_applications_and_follow_ups.sql) crea las tablas y restricciones necesarias.
