package app.powerzone.billing.gateway;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;

/**
 * Adaptador de Stripe Checkout. Los datos de tarjeta se capturan en el
 * checkout alojado de Stripe; la aplicacion solo recibe la URL y el webhook.
 */
public final class StripePaymentGateway implements PaymentGateway {

    private final String secretKey;
    private final String webhookSecret;

    public StripePaymentGateway(String secretKey, String webhookSecret) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String name() {
        return "STRIPE";
    }

    @Override
    public CheckoutSession createCheckout(CheckoutCommand command) {
        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(command.successUrl())
                .setCancelUrl(command.cancelUrl())
                .setClientReferenceId(command.reference())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(command.currency().toLowerCase())
                                .setUnitAmount(command.amountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName(command.planName())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return new CheckoutSession(session.getUrl(), command.expiresAt());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Stripe no pudo crear la sesion de pago: " + ex.getMessage(), ex);
        }
    }

    @Override
    public WebhookVerification verify(String payload, String signature, String timestamp) {
        if (payload == null || signature == null || signature.isBlank()) {
            return WebhookVerification.MISSING_SIGNATURE;
        }
        try {
            Webhook.constructEvent(payload, signature, webhookSecret);
            return WebhookVerification.VALID;
        } catch (SignatureVerificationException ex) {
            return WebhookVerification.INVALID_SIGNATURE;
        } catch (Exception ex) {
            return WebhookVerification.INVALID_SIGNATURE;
        }
    }

    /** La marca de tiempo de Stripe viaja dentro de la cabecera de firma. */
    @Override
    public boolean supportsSandbox() {
        return false;
    }

    Instant now() {
        return Instant.now();
    }
}
