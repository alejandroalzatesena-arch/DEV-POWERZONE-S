# Mapeo de Sprints - PowerZone

Fuente de verdad: Jira **PPP - PowerZone Project Planing** y Notion
"Plan de desarrollo y gobierno de Jira".

Fecha de corte de esta entrega: **28 de noviembre de 2026**.

| Sprint | Fechas | Alcance | Estado |
|---|---|---|---|
| 0 - Foundation | 2026-10-05 → 2026-10-17 | Estructura, API base, esquema, frontend base, CI, IA | Cerrado |
| 1 - Auth y Usuarios | 2026-10-19 → 2026-10-31 | Registro, login, JWT, RBAC, recuperacion | Cerrado |
| 2 - Rutinas | 2026-11-02 → 2026-11-14 | Modelo, motor de rutinas, API, vistas | Cerrado |
| 3 - Pagos | 2026-11-16 → 2026-11-28 | Pasarela, membresias, historial, comprobantes | Cerrado |
| 4 - Administracion y CI | 2026-11-30 → 2026-12-12 | Pendiente | No iniciado |
| 5 - Biblioteca y creador | 2027-01-29 → 2027-02-13 | Pendiente | No iniciado |
| 6 - Tracking | 2027-02-15 → 2027-02-28 | Pendiente | No iniciado |
| 7 - Control de acceso | 2027-03-01 → 2027-03-14 | Pendiente | No iniciado |
| 8 - Clases y reservas | 2027-03-15 → 2027-03-28 | Pendiente | No iniciado |
| 9 - Notificaciones | 2027-03-29 → 2027-04-14 | Pendiente | No iniciado |
| 10 - Inventario y guias | 2027-04-05 → 2027-04-11 | Pendiente | No iniciado |
| 11 - IA y marketing | 2027-04-12 → 2027-04-14 | Pendiente | No iniciado |
| 12 - Reportes y cierre | 2027-03-16 → 2027-04-21 | Pendiente | No iniciado |

## Sprint 0 - Foundation

| Jira | Entregable |
|---|---|
| PPP-1 | Estructura del monorepo, Docker Compose y servicio de IA base |
| PPP-65 | API base: errores RFC 7807, OpenAPI, actuator |
| PPP-66 | Esquema inicial multi-tenant con Flyway |
| PPP-59 | Seguridad base: CORS, headers, rate limit |
| PPP-60 | Frontend Angular 22: routing, design system, layout |
| PPP-74 | Pipeline de CI |
| PPP-304 | Politica de ramas, PR y Conventional Commits |

## Sprint 1 - Auth y Usuarios

| Jira | Entregable |
|---|---|
| PPP-17 | Registro de usuario |
| PPP-18 | Inicio de sesion |
| PPP-57 / PPP-288 | JWT y refresh tokens con rotacion |
| PPP-58 | RBAC por roles |
| PPP-268 / PPP-269 | Sesion segura y "Recordarme" |
| PPP-285 → PPP-291 | Formularios, guards, interceptor y pruebas |

## Sprint 2 - Rutinas

| Jira | Entregable |
|---|---|
| PPP-270 | Modelo de rutinas, catalogo de ejercicios y progreso (Flyway V2) |
| PPP-271 → PPP-273 | Reglas de prescripcion, split y motor de generacion |
| PPP-274 / PPP-278 | API de generacion, consulta semanal, ajuste y archivado |
| PPP-279 | Registro, historial y resumen de progreso |
| PPP-276 / PPP-280 → PPP-282 | Vistas Angular de generacion, rutina diaria y progreso |
| PPP-277 / PPP-284 | 58 pruebas backend y 35 frontend integradas al CI |

## Sprint 3 - Pagos

| Jira | Entregable |
|---|---|
| PPP-22 | Integracion de pasarela de pago |
| PPP-305 / PPP-306 | Migracion V3: planes, pagos, eventos, membresias y comprobantes |
| PPP-307 / PPP-308 | Puerto `PaymentGateway`, sandbox HMAC y adaptador Stripe por configuracion |
| PPP-309 | Checkout, estado de membresia, historial, comprobante PDF y job de expiracion |
| PPP-310 | Pantalla de planes, checkout y metodos de pago en Angular |
| PPP-311 | 85 pruebas de backend del modulo de pagos |
| PPP-23 | Historial de pagos y estado de membresia |
| PPP-312 / PPP-313 | Historial paginado con filtros, permisos por rol y detalle de pago |
| PPP-314 | Comprobante PDF numerado e idempotencia del webhook |
| PPP-315 → PPP-317 | Vistas `/app/payments`, tarjeta de membresia y descarga de comprobantes |
| PPP-318 | 48 pruebas de frontend del dominio de pagos |
| PPP-22 / PPP-23 | Detalle tecnico en `docs/PAYMENTS.md` |

## Pendiente para la siguiente sesion

Comenzar en **Sprint 4 - Administracion y CI** (2026-11-30 → 2026-12-12) usando el
mismo flujo: rama por funcionalidad, PR a `develop`, release a `main` y tag
`sprint-4`.

Antes de continuar, revisar Jira PPP porque los sprints 9-12 tienen fechas solapadas
(PPP-302) y deben replanificarse con el equipo.
