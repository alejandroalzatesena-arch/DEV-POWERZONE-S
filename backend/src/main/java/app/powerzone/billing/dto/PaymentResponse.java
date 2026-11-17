package app.powerzone.billing.dto;

import app.powerzone.billing.Payment;
import java.time.Instant;

public record PaymentResponse(
        long id,
        long planId,
        String planName,
        int amountCents,
        String currency,
        String methodCode,
        String methodLabel,
        String status,
        String reference,
        String checkoutUrl,
        String failureCode,
        String failureMessage,
        int attempt,
        Instant expiresAt,
        Instant completedAt,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(), payment.planId(), payment.planName(), payment.amountCents(),
                payment.currency(), payment.methodCode(), payment.methodLabel(), payment.status(),
                payment.reference(), payment.checkoutUrl(), payment.failureCode(),
                payment.failureMessage(), payment.attempt(), payment.expiresAt(),
                payment.completedAt(), payment.createdAt());
    }
}
