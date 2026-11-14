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

| Tabla | Proposito |
|---|---|
| `exercises` | Catalogo de 29 ejercicios activos por grupo muscular y equipamiento. |
| `routines` | Cabecera del plan semanal: objetivo, experiencia, dias, estado y origen. |
| `routine_days` | Dias del plan con nombre y enfoque, unicos por rutina. |
| `routine_exercises` | Prescripcion por dia: orden, series, repeticiones, descanso y notas. |
| `exercise_progress` | Registro de cargas del miembro, vinculado o no a la rutina. |

### Reglas

- `exercises.muscle_group` y `difficulty` usan `CHECK` para mantener el dominio
  cerrado del motor de generacion.
- `routines.days_per_week` acepta entre 2 y 7 dias; `status` solo admite
  `ACTIVE` y `ARCHIVED`.
- Los dias y ejercicios se eliminan en cascada al borrar la rutina; la rutina se
  archiva en lugar de borrarse para conservar el progreso.
- `exercise_progress.routine_exercise_id` usa `ON DELETE SET NULL`: el historial
  sobrevive a cambios de prescripcion.
- Peso, repeticiones y series se validan en base de datos: 0-500 kg, 1-100 y
  1-10 respectivamente.
- `routines.updated_at` se mantiene con el trigger `trg_routines_updated_at`.
