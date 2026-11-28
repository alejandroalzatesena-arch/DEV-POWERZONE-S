package app.powerzone.billing.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Pasarela simulada para desarrollo y pruebas. Replica el contrato de una
 * pasarela alojada: el checkout vive fuera de la aplicacion y la confirmacion
 * llega por webhook firmado con HMAC-SHA256.
 */
public final class SandboxPaymentGateway implements PaymentGateway {

    public static final String SIGNATURE_PREFIX = "v1=";
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private final String webhookSecret;
    private final String webUrl;
    private final Clock clock;

    public SandboxPaymentGateway(String webhookSecret, String webUrl, Clock clock) {
        this.webhookSecret = webhookSecret;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "SANDBOX";
    }

    @Override
    public CheckoutSession createCheckout(CheckoutCommand command) {
        String url = webUrl + "/app/checkout/" + command.paymentId() + "?sandbox=1";
        return new CheckoutSession(url, command.expiresAt());
    }

    @Override
    public String sign(String payload, String timestamp) {
        return SIGNATURE_PREFIX + hmacHex(timestamp + "." + payload);
    }

    @Override
    public WebhookVerification verify(String payload, String signature, String timestamp) {
        if (payload == null || signature == null || timestamp == null) {
            return WebhookVerification.MISSING_SIGNATURE;
        }
        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            return WebhookVerification.INVALID_SIGNATURE;
        }

        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            return WebhookVerification.INVALID_SIGNATURE;
        }

        long age = Math.abs(clock.instant().getEpochSecond() - sentAt);
        if (age > TOLERANCE.toSeconds()) {
            return WebhookVerification.STALE_TIMESTAMP;
        }

        String expected = sign(payload, timestamp);
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
        return valid ? WebhookVerification.VALID : WebhookVerification.INVALID_SIGNATURE;
    }

    @Override
    public boolean supportsSandbox() {
        return true;
    }

    private String hmacHex(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el webhook simulado", ex);
        }
    }

    /** Solo para pruebas: permite fijar el reloj del verificador. */
    Instant now() {
        return clock.instant();
    }
}
