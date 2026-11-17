package app.powerzone.billing.gateway;

/**
 * Puerto de integracion con la pasarela de pago. El resto del dominio solo
 * conoce este contrato: nunca manipula datos de tarjeta ni secretos.
 */
public interface PaymentGateway {

    /** Nombre tecnico que se guarda en el pago (SANDBOX, STRIPE). */
    String name();

    /** Crea una sesion de checkout alojado. */
    CheckoutSession createCheckout(CheckoutCommand command);

    /**
     * Valida la firma del webhook y la frescura del timestamp para evitar
     * reenvios maliciosos.
     */
    WebhookVerification verify(String payload, String signature, String timestamp);

    /** Firma un payload; solo lo implementan las pasarelas simuladas. */
    default String sign(String payload, String timestamp) {
        throw new UnsupportedOperationException("La pasarela " + name() + " no firma payloads locales");
    }

    /** Indica si el entorno permite simular pagos desde la API. */
    default boolean supportsSandbox() {
        return false;
    }
}
