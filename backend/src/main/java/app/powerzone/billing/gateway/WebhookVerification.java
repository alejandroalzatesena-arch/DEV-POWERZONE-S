package app.powerzone.billing.gateway;

/** Resultado de validar la firma de un webhook. */
public enum WebhookVerification {
    VALID,
    MISSING_SIGNATURE,
    INVALID_SIGNATURE,
    STALE_TIMESTAMP
}
