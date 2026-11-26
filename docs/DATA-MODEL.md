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

## V3 - Pagos y membresias (Sprint 3)

| Tabla | Proposito |
|---|---|
| `membership_plans` | Catalogo de planes por gimnasio: codigo, precio en centavos, moneda y duracion. |
| `payment_methods` | Catalogo global de metodos (`CARD`, `WALLET`, `TRANSFER`) con bandera de cobro inmediato. |
| `gym_payment_methods` | Metodos habilitados por gimnasio; un gimnasio puede desactivar un metodo global. |
| `payments` | Pago local con referencia externa, estado, intento, vencimiento y motivo de rechazo. |
| `payment_events` | Bitacora de webhooks; `event_id` unico es la clave de idempotencia. |
| `memberships` | Vigencia por usuario y gimnasio: plan, `starts_at` y `ends_at`. |
| `payment_receipts` | Numero consecutivo del comprobante asociado a un pago completado. |

### Reglas

- `membership_plans` usa `uq_plans_gym_code` y `CHECK` de precio positivo y
  duracion entre 1 y 730 dias. Los precios se guardan en **centavos** (`INTEGER`)
  para evitar decimales flotantes; la API expone `priceCents` y el frontend
  divide por 100.
- `payments.status` solo admite `PENDING`, `COMPLETED`, `FAILED`, `EXPIRED` y
  `REFUNDED`; `attempt` se acota entre 1 y 99.
- `payments.external_reference` es unico: es la llave con la que el webhook
  encuentra el pago sin confiar en el identificador de la pasarela.
- `payment_events.event_id` es unico. Un reenvio del mismo evento no se procesa
  y el webhook responde `duplicate: true`. `payment_id` usa `ON DELETE SET NULL`
  para conservar la bitacora aunque se borre el pago.
- `memberships` tiene una sola fila por `(gym_id, user_id)`; renovar actualiza
  `ends_at` y `plan_id` en lugar de insertar otra fila. `CHECK` garantiza
  `ends_at > starts_at`.
- `payment_receipts` usa `payment_id` como clave primaria (`ON DELETE RESTRICT`)
  y `payment_receipt_number_seq` inicia en 1000; el numero se genera al emitir
  el comprobante y se reutiliza en descargas posteriores.
- `payments` y `memberships` mantienen `updated_at` con los triggers
  `trg_payments_updated_at` y `trg_memberships_updated_at`.
- Los indices `idx_payments_user_created`, `idx_payments_status_expires` e
  `idx_memberships_ends` sostienen el historial, el job de expiracion y los
  avisos de vencimiento.
- Semillas: tres planes del gimnasio `powerzone-centro` (`MENSUAL` 8.990.000,
  `TRIMESTRAL` 23.990.000 y `ANUAL` 79.990.000 centavos COP) y cuatro metodos
  (`CARD_CREDIT`, `CARD_DEBIT`, `PSE`, `NEQUI`) habilitados para ese gimnasio.
