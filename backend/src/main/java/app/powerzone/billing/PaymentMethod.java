package app.powerzone.billing;

/** Metodo de pago habilitado para un gimnasio. */
public record PaymentMethod(
        String code,
        String label,
        PaymentMethodKind kind,
        boolean immediate) {
}
