package app.powerzone.billing;

import app.powerzone.billing.event.PaymentFailedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca como expirados los pagos pendientes que nunca se confirmaron. La tarea
 * es idempotente y publica el evento de fallo para los demas modulos.
 */
@Component
public class PaymentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PaymentExpiryJob(PaymentRepository paymentRepository,
                            ApplicationEventPublisher eventPublisher,
                            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${powerzone.payments.expiry-scan-ms:60000}",
            initialDelayString = "${powerzone.payments.expiry-scan-ms:60000}")
    @Transactional
    public void expirePending() {
        Instant now = clock.instant();
        List<Long> expired = paymentRepository.findExpiredPending(now, 100);
        for (Long paymentId : expired) {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                continue;
            }
            int updated = paymentRepository.markFailed(
                    paymentId,
                    PaymentStatus.EXPIRED.name(),
                    "PAYMENT_EXPIRED",
                    PaymentWebhookService.friendlyFailure("PAYMENT_EXPIRED"),
                    now);
            if (updated > 0) {
                eventPublisher.publishEvent(new PaymentFailedEvent(
                        paymentId, payment.userId(), payment.gymId(),
                        "PAYMENT_EXPIRED", now));
                log.info("Pago {} expirado por falta de confirmacion", paymentId);
            }
        }
    }
}
