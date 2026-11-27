# PowerZone

Plataforma de gestion integral para gimnasios: registro de miembros, rutinas
personalizadas, seguimiento de progreso, membresias, pagos, control de acceso y
analitica. Proyecto academico del programa **ADSO - SENA**, construido por
sprints con Scrum.

## Stack

| Capa | Tecnologia |
|---|---|
| Frontend | Angular 22 (standalone, signals, zoneless) + SCSS |
| Backend | Java 21 + Spring Boot 3.5 (monolito modular) |
| Base de datos | PostgreSQL 16 + Flyway |
| Servicio de IA | Python 3.12 + FastAPI |
| Tiempo real | WebSocket nativo |
| Documentacion de API | OpenAPI 3 + Swagger UI |
| Infraestructura | Docker Compose + GitHub Actions |

## Estructura

```
backend/       API REST, seguridad y logica de dominio
frontend/      Aplicacion Angular y design system
ai-service/    Microservicio de recomendaciones con FastAPI
docs/          Arquitectura, modelo de datos, ramas y sprints
```

## Puesta en marcha

### Todo con Docker

```bash
cp .env.example .env
docker compose up --build
```

| Servicio | URL |
|---|---|
| Web | http://localhost:4200 |
| API | http://localhost:3000 |
| Swagger UI | http://localhost:3000/api/docs |
| Salud | http://localhost:3000/api/v1/health |
| Servicio de IA | http://localhost:8000/health |

### Backend por separado

Requiere JDK 21 y una instancia de PostgreSQL accesible.

```bash
cd backend
mvn spring-boot:run
```

Variables principales: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`, `PAYMENTS_WEBHOOK_SECRET` y
`STRIPE_SECRET_KEY`. El detalle esta en `.env.example`,
`backend/src/main/resources/application.yml` y `docs/PAYMENTS.md`.

### Frontend por separado

```bash
cd frontend
npm install
npm start
```

El proxy de desarrollo redirige `/api` hacia `http://localhost:3000`.

### Servicio de IA

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## Comandos utiles

| Objetivo | Comando |
|---|---|
| Compilar y probar backend | `cd backend && mvn -B verify` |
| Compilar frontend | `cd frontend && npm run build` |
| Ejecutar pruebas de frontend | `cd frontend && npm test` |
| Levantar base de datos | `docker compose up -d db` |
| Aplicar migraciones | Automatico al iniciar el backend (Flyway) |

## Calidad

- **Conventional Commits** con referencia Jira (`Refs: PPP-000`).
- **Rama por funcionalidad** y pull request obligatorio hacia `develop`.
- **CI** con backend, frontend y servicio de IA en cada push y PR.
- **Errores de API** en formato RFC 7807 (`ProblemDetail`).
- **Migraciones inmutables**: los cambios de esquema se agregan como nuevas
  versiones de Flyway.

Ver `docs/BRANCHING.md` y `docs/COMMITS.md` para el detalle del proceso.

## Documentacion

| Documento | Contenido |
|---|---|
| `docs/ARCHITECTURE.md` | Vision, modulos, multi-tenant y decisiones |
| `docs/DATA-MODEL.md` | Tablas y reglas de persistencia |
| `docs/PAYMENTS.md` | Contrato de pagos, pasarela, membresias y PCI-DSS |
| `docs/SPRINT-MAPPING.md` | Alcance y estado de cada sprint |
| `docs/BRANCHING.md` | Ramas, PR, releases y protecciones |
| `docs/COMMITS.md` | Convencion de mensajes y trazabilidad |

## Estado del proyecto

Entrega acumulada hasta el **Sprint 3** (cerrado al 28 de noviembre de 2026):

| Sprint | Fechas | Estado |
|---|---|---|
| 0 - Foundation | 05 - 17 oct 2026 | Cerrado |
| 1 - Autenticacion y usuarios | 19 - 31 oct 2026 | Cerrado |
| 2 - Rutinas | 02 - 14 nov 2026 | Cerrado |
| 3 - Pagos y membresias | 16 - 28 nov 2026 | Cerrado |

Los sprints 4 a 12 estan planificados en Jira y se implementan en las
siguientes sesiones de trabajo.

### Modulo de pagos

- Pasarela simulada (`SandboxPaymentGateway`) con checkout alojado y webhook
  firmado con HMAC-SHA256 para desarrollo y demostracion.
- `StripePaymentGateway` se activa automaticamente al definir
  `STRIPE_SECRET_KEY`; el resto de la aplicacion no cambia.
- Planes, metodos de pago, historial paginado, estado de membresia y
  comprobante PDF (OpenPDF).
- Idempotencia por `payment_events.event_id` y activacion de membresia en la
  misma transaccion del webhook.
- Variables nuevas: `PAYMENTS_WEBHOOK_SECRET`, `PAYMENTS_CHECKOUT_MINUTES`,
  `PAYMENTS_MAX_ATTEMPTS`, `PAYMENTS_EXPIRING_DAYS`, `PAYMENTS_CHECKOUT_LIMIT`.
  El detalle esta en `docs/PAYMENTS.md`.

## Equipo

| Integrante | Rol |
|---|---|
| Alejandro Alzate | Lider tecnico y arquitectura |
| Juan Jose Guarumo | Backend |
| Juan Diego | Frontend |

## Licencia

Uso academico. Proyecto formativo SENA ADSO, 2026.
