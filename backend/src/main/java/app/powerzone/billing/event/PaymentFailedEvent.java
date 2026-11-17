package app.powerzone.billing.event;

import java.time.Instant;

/** Evento de dominio publicado cuando un pago falla o expira. */
public record PaymentFailedEvent(
        long paymentId,
        long userId,
        long gymId,
        String reason,
        Instant occurredAt) {
}
