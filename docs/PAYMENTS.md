# Pagos y membresias - Sprint 3

Documento de referencia del modulo `app.powerzone.billing` (PPP-22 / PPP-23).
Cubre el contrato HTTP, el ciclo de vida de un pago, la integracion con la
pasarela, el modelo de membresia y las reglas de seguridad y PCI-DSS que aplica
PowerZone.

## Alcance

| Historia | Entregable | Backend | Frontend | QA |
|---|---|---|---|---|
| PPP-22 | Integracion de pasarela de pago | PPP-305 - PPP-309 | PPP-310 | PPP-311 |
| PPP-23 | Historial de pagos y estado de membresia | PPP-312 - PPP-314 | PPP-315 - PPP-317 | PPP-318 |

Fuera de alcance: cobros recurrentes automaticos, facturacion electronica DIAN,
devoluciones parciales y conciliacion contable. El estado `REFUNDED` existe en el
dominio para no rehacer la migracion cuando se habilite la devolucion manual.

## Arquitectura

```
Angular                    Spring Boot                        Pasarela
pz-plans      ─POST /checkout─>  PaymentService  ─createCheckout─>  Sandbox | Stripe
pz-checkout   ─GET /payments/{id}─>  PaymentRepository                │
              <───── redirect ───────────────────────────────────────┘
                                   PaymentWebhookController  <──webhook firmado──
                                        │
                                   PaymentWebhookService (transaccional)
                                        ├── MembershipService.activateOrRenew
                                        ├── ReceiptRepository.issue
                                        └── ApplicationEventPublisher
```

Piezas principales:

| Clase | Responsabilidad |
|---|---|
| `PaymentService` | Checkout, historial, detalle, sandbox y verificacion del webhook |
| `PaymentWebhookService` | Idempotencia, transicion de estado, membresia y comprobante |
| `MembershipService` | Calculo de estado y extension de la vigencia |
| `ReceiptService` / `ReceiptPdfRenderer` | Comprobante PDF con OpenPDF |
| `CheckoutRateLimiter` | Limite de intentos de checkout por usuario y minuto |
| `PaymentExpiryJob` | Marca `EXPIRED` los pagos vencidos cada minuto |
| `PaymentGateway` | Puerto de pasarela; implementaciones `SANDBOX` y `STRIPE` |

## Modelo de estados

```
PENDING ──payment.completed──> COMPLETED ──(devolucion manual)──> REFUNDED
   │
   ├──payment.failed────────> FAILED
   └──expires_at vencido────> EXPIRED
```

- `PENDING` es el unico estado modificable por el webhook.
- `COMPLETED`, `FAILED`, `EXPIRED` y `REFUNDED` son terminales: un evento tardio
  no los altera y responde `IGNORED` o `ALREADY_COMPLETED`.
- La membresia solo se extiende cuando la transicion `PENDING -> COMPLETED`
  afecta exactamente una fila (`UPDATE ... WHERE status = 'PENDING'`). Esto
  protege de webhooks concurrentes sin bloqueos distribuidos.

## Contrato HTTP

Base: `/api/v1/billing`. Todos los endpoints, salvo el webhook, requieren
`Authorization: Bearer <access token>`; el gimnasio se toma del claim `gymId`.

### `GET /plans`

Lista los planes activos del gimnasio ordenados por duracion y precio.

```json
[
  {
    "id": 1,
    "code": "MENSUAL",
    "name": "Plan mensual",
    "description": "Acceso ilimitado al gimnasio durante 30 dias",
    "priceCents": 8990000,
    "currency": "COP",
    "durationDays": 30
  }
]
```

### `GET /payment-methods`

Metodos habilitados para el gimnasio en `gym_payment_methods`.

```json
[
  { "code": "CARD_CREDIT", "label": "Tarjeta de credito", "kind": "CARD", "immediate": true },
  { "code": "PSE", "label": "PSE - transferencia bancaria", "kind": "TRANSFER", "immediate": false }
]
```

### `POST /checkout`

Crea el pago local y devuelve la URL del checkout alojado.

```json
{ "planId": 1, "methodCode": "CARD_CREDIT" }
```

`201 Created` con `Location: /api/v1/billing/payments/{id}`:

```json
{
  "paymentId": 42,
  "reference": "pz_9f1c...",
  "status": "PENDING",
  "checkoutUrl": "http://localhost:4200/app/checkout/42?sandbox=1",
  "amountCents": 8990000,
  "currency": "COP",
  "planName": "Plan mensual",
  "expiresAt": "2026-11-20T15:30:00Z"
}
```

Errores:

| Codigo | HTTP | Causa |
|---|---|---|
| `PLAN_NOT_FOUND` | 404 | Plan inexistente o inactivo |
| `PAYMENT_METHOD_NOT_AVAILABLE` | 400 | Metodo no habilitado para el gimnasio |
| `PAYMENT_RETRY_LIMIT` | 429 | Ya hay `max-attempts` fallos recientes del mismo plan |
| `CHECKOUT_RATE_LIMITED` | 429 | Mas de `checkout-limit` checkouts por minuto |
| `PAYMENT_GATEWAY_ERROR` | 502 | La pasarela no respondio o devolvio un error |

### `GET /payments`

Historial paginado. Un `MEMBER` solo ve sus pagos; `ADMIN`, `TRAINER` y
`SUPER_ADMIN` pueden filtrar con `userId` dentro de su gimnasio.

Parametros: `userId` (solo staff), `from`, `to` (`YYYY-MM-DD`, inclusivos),
`status`, `page` (>= 0, defecto 0), `size` (1 - 50, defecto 10).

```json
{
  "items": [
    {
      "id": 42,
      "planId": 1,
      "planName": "Plan mensual",
      "amountCents": 8990000,
      "currency": "COP",
      "methodCode": "CARD_CREDIT",
      "methodLabel": "Tarjeta de credito",
      "status": "COMPLETED",
      "reference": "pz_9f1c...",
      "checkoutUrl": "http://localhost:4200/app/checkout/42?sandbox=1",
      "attempt": 1,
      "expiresAt": "2026-11-20T15:30:00Z",
      "completedAt": "2026-11-20T15:04:12Z",
      "createdAt": "2026-11-20T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "total": 1,
  "totalPages": 1
}
```

Errores: `INVALID_PAYMENT_STATUS` (400), `INVALID_DATE_RANGE` (400),
`PAYMENT_FORBIDDEN` (403).

### `GET /payments/{paymentId}`

Detalle de un pago. Devuelve `PAYMENT_NOT_FOUND` (404) o `PAYMENT_FORBIDDEN`
(403) si el pago pertenece a otro usuario.

### `GET /payments/{paymentId}/receipt`

Comprobante informativo en PDF (`application/pdf`). Solo para pagos
`COMPLETED`; responde `RECEIPT_NOT_AVAILABLE` (409) en otro estado. El numero de
comprobante sale de la secuencia `payment_receipt_number_seq` (inicia en 1000) y
se conserva entre descargas.

### `GET /membership`

Estado de la membresia calculado en el servidor con la fecha UTC.

```json
{
  "state": "EXPIRING",
  "planName": "Plan mensual",
  "startsAt": "2026-11-01",
  "endsAt": "2026-11-21",
  "daysRemaining": 7,
  "expiringThresholdDays": 7
}
```

| Estado | Regla |
|---|---|
| `NONE` | Sin fila en `memberships` |
| `ACTIVE` | `daysRemaining > expiringThresholdDays` |
| `EXPIRING` | `0 < daysRemaining <= expiringThresholdDays` |
| `EXPIRED` | `endsAt <= today` (la fecha de fin es exclusiva) |

### `POST /payments/{paymentId}/sandbox/pay` y `/sandbox/fail`

Solo disponibles si la pasarela activa expone `supportsSandbox()` (es decir,
`SANDBOX`). En `STRIPE` responden `SANDBOX_DISABLED` (409).

`/sandbox/fail` acepta `{ "failureCode": "INSUFFICIENT_FUNDS" }` (opcional,
por defecto `CARD_DECLINED`). Ambos endpoints construyen el mismo evento firmado
que enviaria la pasarela y lo procesan por `PaymentWebhookService`, de modo que
el camino de prueba y el real son identicos.

### `POST /webhook` (publico)

```http
POST /api/v1/billing/webhook
X-PowerZone-Signature: v1=<hmac-sha256-hex>
X-PowerZone-Timestamp: 1763650800
Content-Type: application/json

{
  "id": "evt_9f1c",
  "type": "payment.completed",
  "data": {
    "reference": "pz_9f1c...",
    "status": "COMPLETED",
    "failureCode": null,
    "failureMessage": null
  }
}
```

Respuesta `200 OK`:

```json
{ "received": true, "duplicate": false, "outcome": "COMPLETED" }
```

`outcome` puede ser `COMPLETED`, `FAILED`, `DUPLICATE`, `ALREADY_COMPLETED`,
`ALREADY_PROCESSED`, `IGNORED`, `REFERENCE_NOT_FOUND`.

Errores: `MISSING_SIGNATURE` / `INVALID_SIGNATURE` / `EMPTY_WEBHOOK` (400),
`STALE_TIMESTAMP` (400), `INVALID_WEBHOOK_PAYLOAD` (400). Las firmas rechazadas
se registran en `payment_events` con `signature_valid = FALSE` para diagnostico.

## Firma del webhook

Algoritmo HMAC-SHA256 sobre `${timestamp}.${payload}` con
`powerzone.payments.webhook-secret` (o `powerzone.stripe.webhook-secret` con
Stripe). La firma viaja como `v1=<hex>`. La ventana de tolerancia es de cinco
minutos para evitar repeticion de eventos capturados.

```bash
# Ejemplo con la pasarela simulada
timestamp=$(date +%s)
payload='{"id":"evt_1","type":"payment.completed","data":{"reference":"pz_abc","status":"COMPLETED"}}'
signature=$(printf '%s.%s' "$timestamp" "$payload" | openssl dgst -sha256 -hmac "$PAYMENTS_WEBHOOK_SECRET" -hex | sed 's/^.* //')
curl -X POST http://localhost:3000/api/v1/billing/webhook \
  -H "Content-Type: application/json" \
  -H "X-PowerZone-Timestamp: $timestamp" \
  -H "X-PowerZone-Signature: v1=$signature" \
  -d "$payload"
```

## Idempotencia

1. `payment_events.event_id` es `UNIQUE`; `tryInsert` falla de forma silenciosa
   ante un reenvio y la respuesta es `duplicate: true`.
2. La transicion de estado usa `UPDATE ... WHERE status = 'PENDING'`; si afecta
   cero filas, otro proceso ya la aplico.
3. La membresia, el comprobante y el evento se confirman en una sola
   transaccion: si falla cualquiera, no queda pago cobrado sin membresia.
4. Los eventos se publican con `ApplicationEventPublisher` despues de la
   escritura; `BillingEventLogger` los deja en el log con `paymentId`,
   `userId`, `amountCents` y referencia, nunca datos de tarjeta.

## Membresia

- Se calcula en el servidor con `LocalDate.now(clock)` en UTC; el cliente solo
  pinta el resultado.
- `activateOrRenew` suma el periodo al `ends_at` vigente si la membresia sigue
  activa; si ya vencio, reinicia desde hoy.
- `ends_at` es **exclusivo**: el dia de vencimiento la membresia ya esta
  `EXPIRED`.
- El umbral de aviso es configurable (`powerzone.payments.expiring-days`, 7 por
  defecto) y se envia al cliente en `expiringThresholdDays` para que la UI no lo
  duplique.

## Configuracion

| Propiedad | Variable | Defecto |
|---|---|---|
| `powerzone.payments.webhook-secret` | `PAYMENTS_WEBHOOK_SECRET` | `powerzone-sandbox-secret` |
| `powerzone.payments.checkout-minutes` | `PAYMENTS_CHECKOUT_MINUTES` | `30` |
| `powerzone.payments.retry-window-hours` | `PAYMENTS_RETRY_WINDOW_HOURS` | `24` |
| `powerzone.payments.max-attempts` | `PAYMENTS_MAX_ATTEMPTS` | `3` |
| `powerzone.payments.expiring-days` | `PAYMENTS_EXPIRING_DAYS` | `7` |
| `powerzone.payments.checkout-limit` | `PAYMENTS_CHECKOUT_LIMIT` | `5` |
| `powerzone.payments.expiry-scan-ms` | `PAYMENTS_EXPIRY_SCAN_MS` | `60000` |
| `powerzone.stripe.secret-key` | `STRIPE_SECRET_KEY` | vacio (usa sandbox) |
| `powerzone.stripe.webhook-secret` | `STRIPE_WEBHOOK_SECRET` | vacio |

La pasarela se elige en `BillingConfig`: si `powerzone.stripe.secret-key` no
esta vacio se instancia `StripePaymentGateway`; en cualquier otro caso se usa
`SandboxPaymentGateway`.

## PCI-DSS y datos sensibles

- PowerZone **no almacena, procesa ni transmite numeros de tarjeta, CVV ni
  fechas de expiracion**. El formulario de tarjeta vive en la pasarela alojada.
- La base de datos solo guarda la referencia externa, el metodo (categoria) y
  el resultado del cobro.
- La cookie `pz_refresh` es `HttpOnly` y `SameSite`; el access token vive en
  memoria y nunca se persiste en `localStorage`.
- Los logs no incluyen el cuerpo del webhook ni la firma completa; solo el
  identificador del evento y el resultado.
- Los comprobantes son informativos y no reemplazan la factura electronica.

## Pruebas

| Suite | Casos |
|---|---|
| `SandboxPaymentGatewayTest` | Firma, ventana de tolerancia, payload alterado |
| `MembershipServiceTest` | Estados, umbral, renovacion y reinicio |
| `PaymentWebhookServiceTest` | Idempotencia, concurrencia, traduccion de rechazos |
| `PaymentServiceTest` | Checkout, permisos, filtros, sandbox y webhook |
| `CheckoutRateLimiterTest` | Limite por usuario y ventana de un minuto |
| `PaymentExpiryJobTest` | Expiracion y publicacion de eventos |
| `ReceiptPdfRendererTest` / `ReceiptServiceTest` | PDF, permisos y numeracion |

En el frontend: `billing.models.spec.ts`, `billing.service.spec.ts`,
`api.service.spec.ts`, `pz-membership-card.spec.ts`, `pz-plans.spec.ts`,
`pz-checkout.spec.ts` y `pz-payments.spec.ts`.

Totales al cierre del Sprint 3: **143 pruebas de backend** y **83 de frontend**.
