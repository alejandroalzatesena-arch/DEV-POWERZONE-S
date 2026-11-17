package app.powerzone.billing.event;

import java.time.Instant;

/** Evento de dominio publicado cuando la pasarela confirma un pago. */
public record PaymentCompletedEvent(
        long paymentId,
        long userId,
        long gymId,
        long planId,
        Instant occurredAt) {
}
