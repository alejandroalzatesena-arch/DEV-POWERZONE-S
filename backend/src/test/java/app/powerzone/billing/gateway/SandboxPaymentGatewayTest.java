package app.powerzone.billing.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SandboxPaymentGatewayTest {

    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");
    private static final String SECRET = "powerzone-sandbox-secret";
    private static final String PAYLOAD = """
            {"id":"evt_1","type":"payment.completed","data":{"reference":"pz_abc"}}
            """;

    private SandboxPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new SandboxPaymentGateway(
                SECRET, "http://localhost:4200/", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("expone el nombre tecnico y soporta el modo sandbox")
    void exposesSandboxContract() {
        assertThat(gateway.name()).isEqualTo("SANDBOX");
        assertThat(gateway.supportsSandbox()).isTrue();
        assertThat(gateway.now()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("crea una URL de checkout alojada con el id del pago")
    void createsHostedCheckout() {
        CheckoutSession session = gateway.createCheckout(new CheckoutCommand(
                42L, "pz_ref", 9_900, "COP", "Mensual", "CARD",
                "ana@powerzone.app", "http://localhost:4200/app/checkout/42",
                "http://localhost:4200/app/plans", NOW.plusSeconds(1800)));

        assertThat(session.checkoutUrl())
                .isEqualTo("http://localhost:4200/app/checkout/42?sandbox=1");
        assertThat(session.expiresAt()).isEqualTo(NOW.plusSeconds(1800));
    }

    @Test
    @DisplayName("firma y valida un webhook vigente")
    void signsAndVerifiesWebhook() {
        String timestamp = String.valueOf(NOW.getEpochSecond());
        String signature = gateway.sign(PAYLOAD, timestamp);

        assertThat(signature).startsWith(SandboxPaymentGateway.SIGNATURE_PREFIX);
        assertThat(gateway.verify(PAYLOAD, signature, timestamp))
                .isEqualTo(WebhookVerification.VALID);
    }

    @Test
    @DisplayName("rechaza payload alterado despues de firmar")
    void rejectsTamperedPayload() {
        String timestamp = String.valueOf(NOW.getEpochSecond());
        String signature = gateway.sign(PAYLOAD, timestamp);

        assertThat(gateway.verify(PAYLOAD + " ", signature, timestamp))
                .isEqualTo(WebhookVerification.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("rechaza firmas sin prefijo o con formato invalido")
    void rejectsMalformedSignature() {
        String timestamp = String.valueOf(NOW.getEpochSecond());

        assertThat(gateway.verify(PAYLOAD, "abc123", timestamp))
                .isEqualTo(WebhookVerification.INVALID_SIGNATURE);
        assertThat(gateway.verify(PAYLOAD, "v1=nohex", timestamp))
                .isEqualTo(WebhookVerification.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("exige firma, cuerpo y timestamp")
    void requiresAllSignatureParts() {
        assertThat(gateway.verify(null, "v1=x", "1"))
                .isEqualTo(WebhookVerification.MISSING_SIGNATURE);
        assertThat(gateway.verify(PAYLOAD, null, "1"))
                .isEqualTo(WebhookVerification.MISSING_SIGNATURE);
        assertThat(gateway.verify(PAYLOAD, "v1=x", null))
                .isEqualTo(WebhookVerification.MISSING_SIGNATURE);
    }

    @Test
    @DisplayName("rechaza timestamps fuera de la ventana de tolerancia")
    void rejectsStaleTimestamp() {
        String stale = String.valueOf(NOW.minusSeconds(301).getEpochSecond());
        String signature = gateway.sign(PAYLOAD, stale);

        assertThat(gateway.verify(PAYLOAD, signature, stale))
                .isEqualTo(WebhookVerification.STALE_TIMESTAMP);
    }

    @Test
    @DisplayName("acepta el limite exacto de la ventana de tolerancia")
    void acceptsBoundaryTimestamp() {
        String boundary = String.valueOf(NOW.minusSeconds(300).getEpochSecond());
        String signature = gateway.sign(PAYLOAD, boundary);

        assertThat(gateway.verify(PAYLOAD, signature, boundary))
                .isEqualTo(WebhookVerification.VALID);
    }

    @Test
    @DisplayName("rechaza un timestamp no numerico como firma invalida")
    void rejectsNonNumericTimestamp() {
        assertThat(gateway.verify(PAYLOAD, "v1=abc", "ayer"))
                .isEqualTo(WebhookVerification.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("la firma depende del secreto configurado")
    void signatureDependsOnSecret() {
        String timestamp = String.valueOf(NOW.getEpochSecond());
        SandboxPaymentGateway other = new SandboxPaymentGateway(
                "otro-secreto", "http://localhost:4200", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(other.sign(PAYLOAD, timestamp))
                .isNotEqualTo(gateway.sign(PAYLOAD, timestamp));
    }
}
