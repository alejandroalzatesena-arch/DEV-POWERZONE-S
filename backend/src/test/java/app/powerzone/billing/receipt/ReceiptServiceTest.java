package app.powerzone.billing.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.billing.Payment;
import app.powerzone.billing.PaymentRepository;
import app.powerzone.billing.PaymentStatus;
import app.powerzone.billing.Receipt;
import app.powerzone.billing.ReceiptRepository;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    private static final long PAYMENT = 42L;
    private static final long GYM = 1L;
    private static final long USER = 5L;
    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");
    private static final byte[] PDF = "%PDF-1.4 test".getBytes();

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GymRepository gymRepository;
    @Mock
    private ReceiptPdfRenderer renderer;

    private ReceiptService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptService(
                paymentRepository, receiptRepository, userRepository, gymRepository, renderer);
    }

    @Test
    @DisplayName("el dueño descarga el comprobante de su pago completado")
    void issuesReceiptForOwner() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(receiptRepository.issue(PAYMENT)).thenReturn(new Receipt(PAYMENT, 7L, NOW));
        when(userRepository.findById(USER)).thenReturn(Optional.of(user()));
        when(gymRepository.findName(GYM)).thenReturn(Optional.of("PowerZone Centro"));
        when(renderer.render(any(ReceiptData.class))).thenReturn(PDF);

        ReceiptDocument document = service.receipt(PAYMENT, USER, GYM, false);

        assertThat(document.content()).isEqualTo(PDF);
        assertThat(document.number()).isEqualTo(7L);

        ArgumentCaptor<ReceiptData> data = ArgumentCaptor.forClass(ReceiptData.class);
        verify(renderer).render(data.capture());
        assertThat(data.getValue().gymName()).isEqualTo("PowerZone Centro");
        assertThat(data.getValue().userName()).isEqualTo("Ana Gomez");
        assertThat(data.getValue().amountCents()).isEqualTo(9_900);
        assertThat(data.getValue().planName()).isEqualTo("Mensual");
    }

    @Test
    @DisplayName("usa un nombre generico si el gimnasio no se encuentra")
    void fallsBackToGenericGymName() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(receiptRepository.issue(PAYMENT)).thenReturn(new Receipt(PAYMENT, 7L, NOW));
        when(userRepository.findById(USER)).thenReturn(Optional.of(user()));
        when(gymRepository.findName(GYM)).thenReturn(Optional.empty());
        when(renderer.render(any(ReceiptData.class))).thenReturn(PDF);

        service.receipt(PAYMENT, USER, GYM, false);

        ArgumentCaptor<ReceiptData> data = ArgumentCaptor.forClass(ReceiptData.class);
        verify(renderer).render(data.capture());
        assertThat(data.getValue().gymName()).isEqualTo("PowerZone");
    }

    @Test
    @DisplayName("el staff del mismo gimnasio tambien puede descargarlo")
    void allowsStaffFromSameGym() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(receiptRepository.issue(PAYMENT)).thenReturn(new Receipt(PAYMENT, 7L, NOW));
        when(userRepository.findById(USER)).thenReturn(Optional.of(user()));
        when(gymRepository.findName(GYM)).thenReturn(Optional.of("PowerZone Centro"));
        when(renderer.render(any(ReceiptData.class))).thenReturn(PDF);

        assertThat(service.receipt(PAYMENT, 99L, GYM, true).number()).isEqualTo(7L);
    }

    @Test
    @DisplayName("un tercero no puede descargar el comprobante")
    void forbidsForeignReceipt() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));

        assertThatThrownBy(() -> service.receipt(PAYMENT, 99L, GYM, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("RECEIPT_FORBIDDEN");
                    assertThat(api.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verifyNoInteractions(receiptRepository, renderer);
    }

    @Test
    @DisplayName("no hay comprobante para pagos no completados")
    void rejectsUnfinishedPayment() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.receipt(PAYMENT, USER, GYM, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("RECEIPT_NOT_AVAILABLE");
                    assertThat(api.status()).isEqualTo(HttpStatus.CONFLICT);
                });
        verifyNoInteractions(receiptRepository, renderer);
    }

    @Test
    @DisplayName("falla si el pago no existe")
    void rejectsUnknownPayment() {
        when(paymentRepository.findById(PAYMENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receipt(PAYMENT, USER, GYM, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("falla si el usuario del pago ya no existe")
    void rejectsMissingUser() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(receiptRepository.issue(PAYMENT)).thenReturn(new Receipt(PAYMENT, 7L, NOW));
        when(userRepository.findById(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receipt(PAYMENT, USER, GYM, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("USER_NOT_FOUND"));
        verifyNoInteractions(renderer);
    }

    private static Payment payment(PaymentStatus status) {
        return new Payment(
                PAYMENT, GYM, USER, 3L, "Mensual", 9_900, "COP", "CARD", "Tarjeta",
                status.name(), "SANDBOX", "pz_ref", null, null, null, 1,
                NOW.plusSeconds(1800), status == PaymentStatus.COMPLETED ? NOW : null,
                NOW.minusSeconds(60));
    }

    private static UserAccount user() {
        return new UserAccount(
                USER, GYM, "ana@powerzone.app", "hash", "Ana", "Gomez", "3001234567",
                "MEMBER", true, LocalDate.of(1998, 3, 14), null, null,
                "STRENGTH", "INTERMEDIATE", 4);
    }
}
