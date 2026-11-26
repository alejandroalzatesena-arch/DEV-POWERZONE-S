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
├── billing/    planes, pagos, pasarela, membresias y comprobantes
├── realtime/   WebSocket nativo (notificaciones y aforo)
└── ...         modulos de sprints futuros (access, classes, reports, ...)
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
- Rate limit adicional por usuario en `POST /api/v1/billing/checkout`.
- El webhook de pagos (`POST /api/v1/billing/webhook`) es publico pero exige
  firma HMAC-SHA256 y ventana de cinco minutos; se registra cada intento en
  `payment_events`.

## Modulo de pagos

`app.powerzone.billing` es un monolito dentro del monolito: expone su contrato
en `PaymentGateway` y selecciona la implementacion en `BillingConfig`.

- `SandboxPaymentGateway` genera un checkout alojado simulado y firma webhooks
  con HMAC-SHA256. Es la pasarela por defecto.
- `StripePaymentGateway` solo se activa cuando `powerzone.stripe.secret-key`
  tiene valor; comparte el mismo puerto y el mismo flujo de webhook.
- `PaymentWebhookService` aplica pago, membresia y comprobante en una sola
  transaccion e idempotente por `payment_events.event_id`.
- `MembershipService` calcula el estado con `Clock` UTC y umbral configurable.
- `PaymentExpiryJob` corre cada minuto y marca `EXPIRED` los `PENDING` vencidos.
- El comprobante es un PDF generado con OpenPDF y numerado por secuencia.

Detalle del contrato, la firma y las reglas PCI-DSS en `docs/PAYMENTS.md`.

## Frontend

- Angular 22 standalone + signals, sin `NgModule`.
- `core/` servicios singleton, interceptores y guards.
- `layouts/` estructura visual; `pages/` vistas por dominio; `shared/` componentes reutilizables.
- Design system propio en SCSS con tokens (`--pz-*`), tema oscuro por defecto.
- Pagos: `/app/plans` (seleccion de plan y metodo), `/app/checkout/:paymentId`
  (confirmacion y sandbox) y `/app/payments` (historial y membresia).
- La tarjeta `pz-membership-card` resume el estado de la membresia en el panel y
  en el historial; ambas pantallas comparten `BillingService`.

## Datos

- PostgreSQL 16.
- Migraciones `V1__...`, `V2__...`, `V3__...` en `backend/src/main/resources/db/migration`.
- Zona horaria de referencia: `America/Bogota`; los calculos de membresia usan
  UTC y las fechas se exponen como `LocalDate`.

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
| 2026-11-16 | Pasarela simulada por defecto y Stripe por configuracion | Permite demostrar el flujo completo sin llaves reales |
| 2026-11-16 | Montos en centavos (`INTEGER`) | Evita errores de redondeo en dinero |
| 2026-11-16 | Webhook idempotente con tabla `payment_events` | Los reenvios de la pasarela no duplican membresias |
| 2026-11-18 | OpenPDF para comprobantes | Licencia LGPL y API simple en Java 21 |
