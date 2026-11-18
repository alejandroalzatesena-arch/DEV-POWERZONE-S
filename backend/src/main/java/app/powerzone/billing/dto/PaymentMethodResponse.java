package app.powerzone.billing.dto;

import app.powerzone.billing.PaymentMethod;

public record PaymentMethodResponse(
        String code,
        String label,
        String kind,
        boolean immediate) {

    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.code(), method.label(), method.kind().name(), method.immediate());
    }
}
