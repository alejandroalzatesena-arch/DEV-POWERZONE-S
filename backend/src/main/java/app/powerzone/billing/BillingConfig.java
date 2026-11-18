package app.powerzone.billing;

import app.powerzone.billing.gateway.PaymentGateway;
import app.powerzone.billing.gateway.SandboxPaymentGateway;
import app.powerzone.billing.gateway.StripePaymentGateway;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la pasarela segun la configuracion: Stripe si hay llave secreta
 * y, en caso contrario, la pasarela simulada para desarrollo y pruebas.
 */
@Configuration
public class BillingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PaymentGateway paymentGateway(
            @Value("${powerzone.stripe.secret-key:}") String stripeSecretKey,
            @Value("${powerzone.stripe.webhook-secret:}") String stripeWebhookSecret,
            @Value("${powerzone.payments.webhook-secret:powerzone-sandbox-secret}") String sandboxSecret,
            @Value("${powerzone.web-url:http://localhost:4200}") String webUrl,
            Clock clock) {
        if (!stripeSecretKey.isBlank()) {
            return new StripePaymentGateway(stripeSecretKey, stripeWebhookSecret);
        }
        return new SandboxPaymentGateway(sandboxSecret, webUrl, clock);
    }
}
