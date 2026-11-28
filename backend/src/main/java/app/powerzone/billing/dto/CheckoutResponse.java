package app.powerzone.billing.dto;

import app.powerzone.billing.Payment;
import java.time.Instant;

public record CheckoutResponse(
        long paymentId,
        String reference,
        String status,
        String checkoutUrl,
        int amountCents,
        String currency,
        String planName,
        Instant expiresAt) {

    public static CheckoutResponse from(Payment payment) {
        return new CheckoutResponse(
                payment.id(), payment.reference(), payment.status(), payment.checkoutUrl(),
                payment.amountCents(), payment.currency(), payment.planName(),
                payment.expiresAt());
    }
}
