package app.powerzone.billing.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de referencia para los modulos de acceso y notificaciones. Solo
 * registra identificadores; nunca datos personales ni de pago.
 */
@Component
public class BillingEventLogger {

    private static final Logger log = LoggerFactory.getLogger(BillingEventLogger.class);

    @EventListener
    public void onCompleted(PaymentCompletedEvent event) {
        log.info("Evento de dominio: pago {} completado para el plan {}",
                event.paymentId(), event.planId());
    }

    @EventListener
    public void onFailed(PaymentFailedEvent event) {
        log.info("Evento de dominio: pago {} fallido ({})", event.paymentId(), event.reason());
    }
}
