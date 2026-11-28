# Changelog

Todas las entregas relevantes de PowerZone. El formato sigue
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
versionado por sprint.

## [0.4.0] - 2026-11-28 - Sprint 3: Pagos y membresias

### Agregado

- Migracion `V3__payments_and_memberships.sql` con planes, metodos de pago,
  pagos, bitacora de eventos, membresias y comprobantes.
- Semillas de tres planes (`MENSUAL`, `TRIMESTRAL`, `ANUAL`) y cuatro metodos
  (`CARD_CREDIT`, `CARD_DEBIT`, `PSE`, `NEQUI`) para el gimnasio semilla.
- Puerto `PaymentGateway` con dos implementaciones: `SandboxPaymentGateway`
  (checkout alojado simulado y webhook HMAC-SHA256) y `StripePaymentGateway`
  (se activa solo al configurar `STRIPE_SECRET_KEY`).
- API de pagos: planes, metodos, checkout, historial paginado con filtros,
  detalle, estado de membresia y comprobante PDF.
- Webhook idempotente con firma HMAC, ventana de tolerancia de cinco minutos y
  bitacora `payment_events`; activacion de membresia y comprobante en una sola
  transaccion.
- Job de expiracion que marca `EXPIRED` los pagos `PENDING` vencidos.
- Rate limit de checkout por usuario e intentos maximos por plan y ventana.
- Comprobante informativo en PDF con OpenPDF y numeracion por secuencia.
- Frontend: `/app/plans` para elegir plan y metodo, `/app/checkout/:paymentId`
  con panel de pruebas y `/app/payments` con historial, filtros y descarga.
- Tarjeta `pz-membership-card` con estado, dias restantes y acceso a renovar,
  integrada al panel y al historial.
- Servicios, modelos y helpers `core/billing` y `ApiService.download()`.
- 85 pruebas nuevas de backend (143 en total) y 48 de frontend (83 en total).

### Cambiado

- El webhook queda publico en la cadena de seguridad pero autenticado por firma.
- Se expone `Content-Disposition` en CORS para que el navegador lea el nombre del
  comprobante.
- La navegacion del panel incorpora Planes y Pagos, y el dashboard muestra la
  membresia vigente.

### Notas

- La pasarela simulada es la opcion por defecto; no se requiere ninguna llave
  externa para recorrer el flujo completo de compra.
- Ningun dato de tarjeta pasa por la aplicacion: el formulario vive en la
  pasarela alojada (ver `docs/PAYMENTS.md`).
- Los montos se manejan en centavos y el frontend los presenta en COP.

## [0.3.0] - 2026-11-14 - Sprint 2: Rutinas y progreso

### Agregado

- Migracion `V2__routine_model.sql` con catalogo de ejercicios, rutinas, dias,
  ejercicios prescritos y progreso por ejercicio.
- Catalogo inicial de 29 ejercicios con grupo muscular, equipamiento y dificultad.
- Reglas de generacion: prescripcion por objetivo y experiencia, split de 2 a 7
  dias y cantidad de ejercicios por sesion.
- Motor de generacion que valida el perfil, reparte ejercicios del catalogo sin
  repetirlos en la misma sesion y equilibra el volumen semanal.
- API de rutinas: generacion (`POST /api/v1/routines/generate`), listado,
  detalle, semana y dia, ajuste de prescripcion para ADMIN/TRAINER y archivado.
- Registro de progreso (`POST /api/v1/progress`), historial con filtros y
  resumen por ejercicio con mejor carga y ultima sesion.
- Frontend: generador de rutinas con sincronizacion del perfil, listado, vista
  semanal, registro de progreso desde la sesion diaria y pantalla `/app/progress`.
- Servicios y modelos Angular de rutinas y progreso.
- 46 pruebas nuevas de backend (58 en total) y 21 de frontend (35 en total).

### Cambiado

- La navegacion del panel incorpora la seccion Progreso.
- El detalle de rutina permite registrar cargas sin salir de la sesion.

### Notas

- Entrega del alcance implementado al corte del 4 de noviembre de 2026. El
  Sprint 2 permanece en curso en Jira hasta el 14 de noviembre.
- El Sprint 3 (pagos) se implementa en la siguiente sesion de trabajo.

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
