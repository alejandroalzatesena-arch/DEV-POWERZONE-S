package app.powerzone.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.billing.dto.WebhookEvent;
import app.powerzone.billing.dto.WebhookResponse;
import app.powerzone.billing.event.PaymentCompletedEvent;
import app.powerzone.billing.event.PaymentFailedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    private static final long PAYMENT_ID = 42L;
    private static final long GYM = 1L;
    private static final long USER = 5L;
    private static final long PLAN = 3L;
    private static final String REFERENCE = "pz_abc123";
    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventRepository eventRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private MembershipService membershipService;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentWebhookService service;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookService(
                paymentRepository, eventRepository, planRepository, membershipService,
                receiptRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("un evento repetido responde como duplicado sin reprocesar")
    void ignoresDuplicatedEvent() {
        when(eventRepository.tryInsert("evt_1", "payment.completed")).thenReturn(false);

        WebhookResponse response = service.process(completed("evt_1"));

        assertThat(response.received()).isTrue();
        assertThat(response.duplicate()).isTrue();
        assertThat(response.outcome()).isEqualTo("DUPLICATE");
        verifyNoInteractions(paymentRepository, membershipService, receiptRepository, eventPublisher);
    }

    @Test
    @DisplayName("una referencia desconocida se registra y no rompe el webhook")
    void handlesUnknownReference() {
        when(eventRepository.tryInsert("evt_1", "payment.completed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.empty());

        WebhookResponse response = service.process(completed("evt_1"));

        assertThat(response.duplicate()).isFalse();
        assertThat(response.outcome()).isEqualTo("REFERENCE_NOT_FOUND");
        verify(eventRepository).finish("evt_1", null, "REFERENCE_NOT_FOUND");
        verifyNoInteractions(membershipService, receiptRepository, eventPublisher);
    }

    @Test
    @DisplayName("un pago completado activa membresia, emite comprobante y publica evento")
    void completesPaymentAtomically() {
        when(eventRepository.tryInsert("evt_1", "payment.completed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        when(paymentRepository.markCompleted(PAYMENT_ID, NOW)).thenReturn(1);
        when(planRepository.findActiveById(PLAN, GYM))
                .thenReturn(Optional.of(new Plan(
                        PLAN, GYM, "MONTHLY", "Mensual", "Acceso mensual",
                        9_900, "COP", 30, true)));

        WebhookResponse response = service.process(completed("evt_1"));

        assertThat(response.outcome()).isEqualTo("COMPLETED");
        verify(membershipService).activateOrRenew(GYM, USER, PLAN, 30);
        verify(receiptRepository).issue(PAYMENT_ID);
        verify(eventRepository).finish("evt_1", PAYMENT_ID, "COMPLETED");

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentCompletedEvent.class);
        PaymentCompletedEvent completed = (PaymentCompletedEvent) event.getValue();
        assertThat(completed.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(completed.userId()).isEqualTo(USER);
        assertThat(completed.occurredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("un pago ya completado no vuelve a extender la membresia")
    void doesNotReprocessCompletedPayment() {
        when(eventRepository.tryInsert("evt_2", "payment.completed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));

        WebhookResponse response = service.process(completed("evt_2"));

        assertThat(response.outcome()).isEqualTo("ALREADY_COMPLETED");
        verify(paymentRepository, never()).markCompleted(anyLong(), any());
        verifyNoInteractions(membershipService, receiptRepository, eventPublisher);
        verify(eventRepository).finish("evt_2", PAYMENT_ID, "ALREADY_COMPLETED");
    }

    @Test
    @DisplayName("un pago fallido no se puede completar")
    void cannotCompleteFailedPayment() {
        when(eventRepository.tryInsert("evt_3", "payment.completed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.FAILED)));

        WebhookResponse response = service.process(completed("evt_3"));

        assertThat(response.outcome()).isEqualTo("IGNORED");
        verify(paymentRepository, never()).markCompleted(anyLong(), any());
        verifyNoInteractions(membershipService, receiptRepository, eventPublisher);
    }

    @Test
    @DisplayName("una actualizacion concurrente del pago no duplica efectos")
    void handlesConcurrentCompletion() {
        when(eventRepository.tryInsert("evt_4", "payment.completed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        when(paymentRepository.markCompleted(PAYMENT_ID, NOW)).thenReturn(0);

        WebhookResponse response = service.process(completed("evt_4"));

        assertThat(response.outcome()).isEqualTo("ALREADY_PROCESSED");
        verifyNoInteractions(membershipService, receiptRepository, eventPublisher);
    }

    @Test
    @DisplayName("un evento sin estado reconocible se ignora sin tocar el pago")
    void ignoresUnsupportedEvent() {
        when(eventRepository.tryInsert("evt_5", "payment.updated")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        WebhookResponse response = service.process(new WebhookEvent(
                "evt_5", "payment.updated",
                new WebhookEvent.Data(REFERENCE, "PROCESSING", null, null)));

        assertThat(response.outcome()).isEqualTo("IGNORED");
        verify(paymentRepository, never()).markCompleted(anyLong(), any());
        verify(paymentRepository, never()).markFailed(anyLong(), any(), any(), any(), any());
        verify(eventRepository).finish("evt_5", PAYMENT_ID, "IGNORED");
    }

    @Test
    @DisplayName("un pago rechazado guarda el motivo y publica el evento")
    void failsPayment() {
        when(eventRepository.tryInsert("evt_6", "payment.failed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        String message = PaymentWebhookService.friendlyFailure("INSUFFICIENT_FUNDS");
        when(paymentRepository.markFailed(
                PAYMENT_ID, "FAILED", "INSUFFICIENT_FUNDS", message, NOW)).thenReturn(1);

        WebhookResponse response = service.process(failed("evt_6", "INSUFFICIENT_FUNDS"));

        assertThat(response.outcome()).isEqualTo("FAILED");
        verify(eventRepository).finish("evt_6", PAYMENT_ID, "FAILED");

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentFailedEvent.class);
        assertThat(((PaymentFailedEvent) event.getValue()).reason())
                .isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    @DisplayName("un rechazo sin codigo usa PAYMENT_DECLINED")
    void defaultsFailureCode() {
        when(eventRepository.tryInsert("evt_7", "payment.failed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        String message = PaymentWebhookService.friendlyFailure("PAYMENT_DECLINED");
        when(paymentRepository.markFailed(
                PAYMENT_ID, "FAILED", "PAYMENT_DECLINED", message, NOW)).thenReturn(1);

        WebhookResponse response = service.process(failed("evt_7", null));

        assertThat(response.outcome()).isEqualTo("FAILED");
        verify(paymentRepository).markFailed(
                PAYMENT_ID, "FAILED", "PAYMENT_DECLINED", message, NOW);
    }

    @Test
    @DisplayName("un pago ya resuelto no cambia de estado por un rechazo tardio")
    void ignoresFailureForResolvedPayment() {
        when(eventRepository.tryInsert("evt_8", "payment.failed")).thenReturn(true);
        when(paymentRepository.findByReference(REFERENCE))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));

        WebhookResponse response = service.process(failed("evt_8", "CARD_DECLINED"));

        assertThat(response.outcome()).isEqualTo("IGNORED");
        verify(paymentRepository, never()).markFailed(anyLong(), any(), any(), any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("reconoce estados equivalentes de la pasarela")
    void recognizesGatewayStatuses() {
        assertThat(PaymentWebhookService.isCompleted(
                new WebhookEvent("e", "payment.updated",
                        new WebhookEvent.Data(REFERENCE, "APPROVED", null, null)))).isTrue();
        assertThat(PaymentWebhookService.isCompleted(
                new WebhookEvent("e", "payment.updated",
                        new WebhookEvent.Data(REFERENCE, "SUCCEEDED", null, null)))).isTrue();
        assertThat(PaymentWebhookService.isFailed(
                new WebhookEvent("e", "payment.updated",
                        new WebhookEvent.Data(REFERENCE, "DECLINED", null, null)))).isTrue();
        assertThat(PaymentWebhookService.isCompleted(
                new WebhookEvent("e", "payment.updated",
                        new WebhookEvent.Data(REFERENCE, "PROCESSING", null, null)))).isFalse();
        assertThat(PaymentWebhookService.isFailed(
                new WebhookEvent("e", "payment.updated",
                        new WebhookEvent.Data(REFERENCE, null, null, null)))).isFalse();
    }

    @Test
    @DisplayName("traduce los codigos de rechazo a mensajes claros")
    void translatesFailureCodes() {
        assertThat(PaymentWebhookService.friendlyFailure("insufficient_funds"))
                .contains("Fondos insuficientes");
        assertThat(PaymentWebhookService.friendlyFailure("EXPIRED_CARD"))
                .contains("vencida");
        assertThat(PaymentWebhookService.friendlyFailure("PAYMENT_EXPIRED"))
                .contains("expiro");
        assertThat(PaymentWebhookService.friendlyFailure(null))
                .contains("no se pudo completar");
    }

    private static WebhookEvent completed(String eventId) {
        return new WebhookEvent(eventId, "payment.completed",
                new WebhookEvent.Data(REFERENCE, "COMPLETED", null, null));
    }

    private static WebhookEvent failed(String eventId, String failureCode) {
        return new WebhookEvent(eventId, "payment.failed",
                new WebhookEvent.Data(REFERENCE, "FAILED", failureCode, null));
    }

    private static Payment payment(PaymentStatus status) {
        return new Payment(
                PAYMENT_ID, GYM, USER, PLAN, "Mensual", 9_900, "COP",
                "CARD", "Tarjeta", status.name(), "SANDBOX", REFERENCE,
                "http://localhost:4200/app/checkout/42?sandbox=1", null, null, 1,
                NOW.plusSeconds(1800), status == PaymentStatus.COMPLETED ? NOW : null,
                NOW.minusSeconds(60));
    }
}
