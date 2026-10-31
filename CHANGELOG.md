# Changelog

Todas las entregas relevantes de PowerZone. El formato sigue
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
versionado por sprint.

## [0.2.0] - 2026-10-31 - Sprint 1: Autenticacion y usuarios

### Agregado

- Registro de miembros con perfil corporal y de entrenamiento
  (`POST /api/v1/auth/register`).
- Validacion de email unico por gimnasio, contrasena de 8 a 72 caracteres con
  letra y numero, y cifrado BCrypt factor 12.
- Correo de confirmacion de registro mediante el puerto `MailService`.
- Autenticacion JWT HS256 con access token de 15 minutos.
- Refresh tokens opacos con hash SHA-256, rotacion en cada uso y cookie
  `pz_refresh` HttpOnly, SameSite Lax.
- Login, refresh, logout y bloqueo temporal tras cinco intentos fallidos en
  quince minutos.
- RBAC: perfil propio (`/users/me`), directorio para ADMIN y TRAINER y cambio
  de rol exclusivo de ADMIN.
- Politica de sesiones: maximo cinco activas, opcion "Recordarme" de 30 dias y
  limpieza programada de credenciales vencidas.
- Recuperacion de contrasena con token de un solo uso y revocacion de sesiones.
- Frontend: pantallas de login, registro, recuperacion y nueva contrasena con
  validaciones en cliente.
- Interceptor HTTP con renovacion automatica de sesion y guards de ruta.
- Barra superior con usuario, rol y cierre de sesion.
- 12 pruebas de backend (Mockito) y 14 de frontend (Vitest + jsdom) integradas
  al pipeline.

### Cambiado

- La API queda cerrada por defecto: solo salud, documentacion y endpoints de
  autenticacion son publicos.
- Los errores 401 y 403 se emiten en formato RFC 7807.
- El pipeline de CI ejecuta pruebas unitarias de Angular antes de publicar el
  bundle.

### Notas

- El adaptador de correo de desarrollo escribe el contenido en el log; el
  adaptador SMTP se configura en el sprint de notificaciones.
- El modelo y motor de rutinas se incorporan en el Sprint 2.

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
