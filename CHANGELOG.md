# Changelog

Todas las entregas relevantes de PowerZone. El formato sigue
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
versionado por sprint.

## [0.1.0] - 2026-10-17 - Sprint 0: Foundation

### Agregado

- Monorepo con `backend/`, `frontend/`, `ai-service/` y `docs/`.
- Backend Spring Boot 3.5 sobre Java 21 con modulo `common` para errores,
  OpenAPI y salud.
- Manejo global de errores con `ProblemDetail` (RFC 7807) y codigos de negocio.
- Documentacion OpenAPI con Swagger UI en `/api/docs`.
- Esquema multi-tenant inicial con Flyway: `gyms`, `roles`, `users`,
  `refresh_tokens` y `password_reset_tokens`.
- Frontend Angular 22 zoneless con routing diferido, layout de panel y layout
  de autenticacion.
- Design system propio: tokens `--pz-*` y componentes de boton, tarjeta, badge,
  estado vacio y notificaciones.
- Servicios `ApiService`, `TokenService` y `ToastService`.
- Seguridad base: cadena stateless, CORS configurable, cabeceras de seguridad,
  BCrypt y rate limiting por IP.
- Servicio de IA en FastAPI con healthcheck y stub de sugerencias.
- Docker Compose con PostgreSQL 16, backend, frontend y servicio de IA.
- Pipeline de CI para backend, frontend y servicio de IA.
- Politica de ramas, convencion de commits y plantilla de pull request.

### Notas

- La autenticacion JWT y el RBAC se incorporan en el Sprint 1.
- El modelo y motor de rutinas se incorporan en el Sprint 2.
