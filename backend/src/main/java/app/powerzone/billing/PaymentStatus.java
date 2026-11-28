package app.powerzone.billing;

/** Estados posibles de un pago local. */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    EXPIRED,
    REFUNDED;

    public static PaymentStatus from(String value) {
        return value == null ? null : PaymentStatus.valueOf(value);
    }

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED || this == REFUNDED;
    }
}
