# Arquitectura - PowerZone

## Vision general

PowerZone es un sistema de gestion de gimnasios construido como **monolito modular**.
Cada dominio vive en su propio paquete y solo se comunica con los demas mediante
interfaces de servicio, lo que permite extraer un modulo a un servicio independiente
sin reescribir el resto.

```
Angular 22  ──HTTP/JSON──>  Spring Boot 3.5 (monolito modular)  ──JDBC──>  PostgreSQL 16
     │                              │
     └──────WebSocket───────────────┘
                                    │
                                    └──HTTP──> FastAPI (servicio de IA)
```

## Backend

```
app.powerzone
├── common/     configuracion transversal: errores, OpenAPI, seguridad, rate limit
├── auth/       registro, login, JWT, refresh tokens, recuperacion
├── users/      perfiles, roles y administracion de usuarios
├── routines/   catalogo de ejercicios, motor de generacion, progreso
├── realtime/   WebSocket nativo (notificaciones y aforo)
└── ...         modulos de sprints futuros (payments, memberships, ...)
```

Reglas:

- Controladores sin logica de negocio: delegan en servicios.
- Acceso a datos con `JdbcTemplate` y repositorios explicitos (sin JPA/Hibernate).
- DTOs como `record` de Java 21; las entidades nunca salen del backend.
- Errores con `ProblemDetail` (RFC 7807) y un `@RestControllerAdvice` comun.
- Versionado de API en la ruta: `/api/v1/...`.

## Multi-tenant

- Tabla `gyms` como raiz del tenant.
- Todas las tablas de negocio llevan `gym_id`.
- El gimnasio activo se resuelve por request (claim del JWT o cabecera para super-admin)
  y se aplica como filtro obligatorio en los repositorios.
- Flyway mantiene el esquema; nunca se modifican migraciones ya aplicadas.

## Seguridad

- Contrasenas con BCrypt.
- Access token JWT corto (15 min) + refresh token opaco en base de datos con rotacion.
- Cookies `HttpOnly`/`SameSite` para el refresh token; access token en memoria del cliente.
- RBAC con roles `MEMBER`, `TRAINER`, `ADMIN`, `SUPER_ADMIN`.
- Rate limiting por IP y cabeceras de seguridad en todas las respuestas.

## Frontend

- Angular 22 standalone + signals, sin `NgModule`.
- `core/` servicios singleton, interceptores y guards.
- `layouts/` estructura visual; `pages/` vistas por dominio; `shared/` componentes reutilizables.
- Design system propio en SCSS con tokens (`--pz-*`), tema oscuro por defecto.

## Datos

- PostgreSQL 16.
- Migraciones `V1__...`, `V2__...` en `backend/src/main/resources/db/migration`.
- Zona horaria de referencia: `America/Bogota`.

## Servicio de IA

FastAPI independiente, autenticado con `X-API-Key`. En sprints iniciales solo expone
salud y un stub de sugerencias; el motor real llega en el Sprint 11.

## Entornos

| Entorno | Base de datos | API | Web |
|---|---|---|---|
| Local | Docker `db` (5432) | `localhost:3000` | `localhost:4200` |
| Integracion | Docker Compose | `backend:3000` | `frontend:80` |
| Produccion | Gestionada | Detras de proxy | Estatico |

## Decisiones

| Fecha | Decision | Motivo |
|---|---|---|
| 2026-10-05 | Monolito modular en lugar de microservicios | Equipo de 3 personas; menor costo operativo |
| 2026-10-05 | JDBC explicito en lugar de JPA | Control de SQL y del aislamiento multi-tenant |
| 2026-10-05 | Angular standalone + signals | Estado simple sin librerias externas |
