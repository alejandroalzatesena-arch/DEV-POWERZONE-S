package app.powerzone.billing;

import app.powerzone.billing.event.PaymentCompletedEvent;
import app.powerzone.billing.event.PaymentFailedEvent;
import app.powerzone.billing.dto.WebhookEvent;
import app.powerzone.billing.dto.WebhookResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa los eventos de la pasarela de forma transaccional e idempotente: el
 * pago, la membresia y el comprobante se confirman en una sola transaccion.
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final PlanRepository planRepository;
    private final MembershipService membershipService;
    private final ReceiptRepository receiptRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PaymentWebhookService(PaymentRepository paymentRepository,
                                 PaymentEventRepository eventRepository,
                                 PlanRepository planRepository,
                                 MembershipService membershipService,
                                 ReceiptRepository receiptRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 Clock clock) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.planRepository = planRepository;
        this.membershipService = membershipService;
        this.receiptRepository = receiptRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public WebhookResponse process(WebhookEvent event) {
        if (!eventRepository.tryInsert(event.id(), event.type())) {
            return new WebhookResponse(true, true, "DUPLICATE");
        }

        Payment payment = paymentRepository.findByReference(event.data().reference())
                .orElse(null);
        if (payment == null) {
            eventRepository.finish(event.id(), null, "REFERENCE_NOT_FOUND");
            return new WebhookResponse(true, false, "REFERENCE_NOT_FOUND");
        }

        String outcome;
        if (isCompleted(event)) {
            outcome = complete(payment);
        } else if (isFailed(event)) {
            outcome = fail(payment, event.data().failureCode());
        } else {
            outcome = "IGNORED";
        }

        eventRepository.finish(event.id(), payment.id(), outcome);
        return new WebhookResponse(true, false, outcome);
    }

    private String complete(Payment payment) {
        if (payment.statusValue() == PaymentStatus.COMPLETED) {
            return "ALREADY_COMPLETED";
        }
        if (payment.statusValue() != PaymentStatus.PENDING) {
            return "IGNORED";
        }

        Instant now = clock.instant();
        if (paymentRepository.markCompleted(payment.id(), now) == 0) {
            return "ALREADY_PROCESSED";
        }

        Plan plan = planRepository.findActiveById(payment.planId(), payment.gymId())
                .orElseThrow(() -> new IllegalStateException(
                        "El plan del pago " + payment.id() + " no esta disponible"));
        membershipService.activateOrRenew(
                payment.gymId(), payment.userId(), payment.planId(), plan.durationDays());
        receiptRepository.issue(payment.id());
        eventPublisher.publishEvent(new PaymentCompletedEvent(
                payment.id(), payment.userId(), payment.gymId(), payment.planId(), now));
        return "COMPLETED";
    }

    private String fail(Payment payment, String failureCode) {
        if (payment.statusValue() != PaymentStatus.PENDING) {
            return "IGNORED";
        }

        String code = normalizeCode(failureCode);
        Instant now = clock.instant();
        int updated = paymentRepository.markFailed(
                payment.id(), PaymentStatus.FAILED.name(), code, friendlyFailure(code), now);
        if (updated == 0) {
            return "ALREADY_PROCESSED";
        }

        eventPublisher.publishEvent(new PaymentFailedEvent(
                payment.id(), payment.userId(), payment.gymId(), code, now));
        return "FAILED";
    }

    static boolean isCompleted(WebhookEvent event) {
        return "payment.completed".equalsIgnoreCase(event.type())
                || statusIn(event, "COMPLETED", "APPROVED", "SUCCEEDED");
    }

    static boolean isFailed(WebhookEvent event) {
        return "payment.failed".equalsIgnoreCase(event.type())
                || statusIn(event, "FAILED", "DECLINED", "REJECTED");
    }

    private static boolean statusIn(WebhookEvent event, String... values) {
        if (event.data() == null || event.data().status() == null) {
            return false;
        }
        for (String value : values) {
            if (value.equalsIgnoreCase(event.data().status())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCode(String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            return "PAYMENT_DECLINED";
        }
        return failureCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Traduce codigos tecnicos de la pasarela a mensajes para el usuario. */
    public static String friendlyFailure(String failureCode) {
        return switch (normalizeCode(failureCode)) {
            case "INSUFFICIENT_FUNDS" -> "Fondos insuficientes. Prueba con otro metodo de pago.";
            case "CARD_DECLINED" -> "La tarjeta fue rechazada por el banco. Revisa los datos o usa otra tarjeta.";
            case "EXPIRED_CARD" -> "La tarjeta esta vencida. Usa una tarjeta vigente.";
            case "TIMEOUT" -> "La pasarela no respondio a tiempo. Puedes reintentar el pago.";
            case "PAYMENT_EXPIRED" -> "El pago expiro sin confirmarse. Genera uno nuevo para continuar.";
            case "GATEWAY_ERROR" -> "La pasarela no esta disponible en este momento. Intenta de nuevo en unos minutos.";
            default -> "El pago no se pudo completar. Puedes intentarlo de nuevo.";
        };
    }
}
