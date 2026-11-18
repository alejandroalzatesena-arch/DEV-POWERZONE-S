package app.powerzone.billing;

import app.powerzone.billing.dto.WebhookResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico de webhooks: queda fuera de la autenticacion JWT pero
 * exige la firma HMAC de la pasarela y responde rapido para evitar reenvios.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public WebhookResponse webhook(
            @RequestHeader(value = "X-PowerZone-Signature", required = false) String signature,
            @RequestHeader(value = "X-PowerZone-Timestamp", required = false) String timestamp,
            @RequestBody(required = false) String payload) {
        return paymentService.handleWebhook(payload, signature, timestamp);
    }
}
