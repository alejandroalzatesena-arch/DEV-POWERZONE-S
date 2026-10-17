# Modelo de datos

Motor: PostgreSQL 16. Migraciones versionadas con Flyway.

## V1 - Foundation

| Tabla | Proposito |
|---|---|
| `gyms` | Raiz del tenant. Todo dato de negocio referencia un gimnasio. |
| `roles` | Catalogo de roles RBAC: `MEMBER`, `TRAINER`, `ADMIN`, `SUPER_ADMIN`. |
| `users` | Cuentas y perfil fisico usado por el motor de rutinas. |
| `refresh_tokens` | Refresh tokens opacos, hasheados y revocables. |
| `password_reset_tokens` | Tokens de recuperacion de contrasena de un solo uso. |

### Reglas

- El email es unico **por gimnasio** (`uq_users_gym_email`), no global.
- `password_hash` almacena BCrypt; nunca texto plano.
- `goal` y `experience_level` estan restringidos por `CHECK` para que el motor de
  rutinas no reciba valores fuera del dominio.
- `updated_at` se mantiene con el trigger `trg_users_updated_at`.

## V2 - Rutinas (Sprint 2)

Se incorpora en la rama `feat/PPP-270-routine-model`: catalogo de ejercicios,
rutinas, dias, ejercicios de rutina y progreso por ejercicio.
