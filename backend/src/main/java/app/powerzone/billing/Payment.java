package app.powerzone.billing;

import java.time.Instant;

/**
 * Pago local. Solo guarda la referencia de la pasarela y metadatos del cobro:
 * ningun dato de tarjeta pasa por la base de datos ni por los logs.
 */
public record Payment(
        long id,
        long gymId,
        long userId,
        long planId,
        String planName,
        int amountCents,
        String currency,
        String methodCode,
        String methodLabel,
        String status,
        String gateway,
        String reference,
        String checkoutUrl,
        String failureCode,
        String failureMessage,
        int attempt,
        Instant expiresAt,
        Instant completedAt,
        Instant createdAt) {

    public PaymentStatus statusValue() {
        return PaymentStatus.from(status);
    }
}
