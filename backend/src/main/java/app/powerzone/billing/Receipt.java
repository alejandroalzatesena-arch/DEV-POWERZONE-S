package app.powerzone.billing;

import java.time.Instant;

/** Comprobante numerado de un pago completado. */
public record Receipt(long paymentId, long number, Instant issuedAt) {
}
