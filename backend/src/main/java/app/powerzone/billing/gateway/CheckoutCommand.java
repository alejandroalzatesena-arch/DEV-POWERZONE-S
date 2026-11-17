package app.powerzone.billing.gateway;

import java.time.Instant;

/** Datos minimos para abrir una sesion de pago alojada en la pasarela. */
public record CheckoutCommand(
        long paymentId,
        String reference,
        long amountCents,
        String currency,
        String planName,
        String methodCode,
        String userEmail,
        String successUrl,
        String cancelUrl,
        Instant expiresAt) {
}
