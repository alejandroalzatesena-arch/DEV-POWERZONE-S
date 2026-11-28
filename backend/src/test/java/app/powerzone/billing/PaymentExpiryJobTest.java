package app.powerzone.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.powerzone.billing.event.PaymentFailedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
class PaymentExpiryJobTest {

    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentExpiryJob job;

    @BeforeEach
    void setUp() {
        job = new PaymentExpiryJob(
                paymentRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("marca como EXPIRED los pagos vencidos y publica el evento")
    void expiresPendingPayments() {
        when(paymentRepository.findExpiredPending(NOW, 100)).thenReturn(List.of(1L));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(1L)));
        String message = PaymentWebhookService.friendlyFailure("PAYMENT_EXPIRED");
        when(paymentRepository.markFailed(
                1L, "EXPIRED", "PAYMENT_EXPIRED", message, NOW)).thenReturn(1);

        job.expirePending();

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(PaymentFailedEvent.class);
        PaymentFailedEvent failed = (PaymentFailedEvent) event.getValue();
        assertThat(failed.paymentId()).isEqualTo(1L);
        assertThat(failed.reason()).isEqualTo("PAYMENT_EXPIRED");
        assertThat(failed.occurredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("no publica evento si otro proceso ya resolvio el pago")
    void skipsAlreadyResolvedPayment() {
        when(paymentRepository.findExpiredPending(NOW, 100)).thenReturn(List.of(1L));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(1L)));
        when(paymentRepository.markFailed(anyLong(), anyString(), anyString(), anyString(), any()))
                .thenReturn(0);

        job.expirePending();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("ignora un pago eliminado entre la consulta y la actualizacion")
    void skipsMissingPayment() {
        when(paymentRepository.findExpiredPending(NOW, 100)).thenReturn(List.of(1L));
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        job.expirePending();

        verify(paymentRepository, never()).markFailed(
                anyLong(), anyString(), anyString(), anyString(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("sin pagos vencidos no hace ninguna escritura")
    void doesNothingWithoutExpiredPayments() {
        when(paymentRepository.findExpiredPending(NOW, 100)).thenReturn(List.of());

        job.expirePending();

        verify(paymentRepository, never()).markFailed(
                eq(1L), anyString(), anyString(), anyString(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static Payment payment(long id) {
        return new Payment(
                id, 1L, 5L, 3L, "Mensual", 9_900, "COP", "CARD", "Tarjeta",
                "PENDING", "SANDBOX", "pz_ref", null, null, null, 1,
                NOW.minusSeconds(60), null, NOW.minusSeconds(3600));
    }
}
