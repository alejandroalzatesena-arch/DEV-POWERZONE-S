package app.powerzone.billing;

/** Familia del metodo de pago; determina si la confirmacion es inmediata o diferida. */
public enum PaymentMethodKind {
    CARD,
    WALLET,
    TRANSFER
}
