package app.powerzone.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.billing.dto.CheckoutRequest;
import app.powerzone.billing.dto.CheckoutResponse;
import app.powerzone.billing.dto.PageResponse;
import app.powerzone.billing.dto.PaymentResponse;
import app.powerzone.billing.dto.WebhookEvent;
import app.powerzone.billing.dto.WebhookResponse;
import app.powerzone.billing.gateway.CheckoutCommand;
import app.powerzone.billing.gateway.CheckoutSession;
import app.powerzone.billing.gateway.PaymentGateway;
import app.powerzone.billing.gateway.WebhookVerification;
import app.powerzone.common.ApiException;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long GYM = 1L;
    private static final long USER = 5L;
    private static final long PLAN = 3L;
    private static final long PAYMENT = 42L;
    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");
    private static final Instant EXPIRES = NOW.plusSeconds(1800);

    @Mock
    private PlanRepository planRepository;
    @Mock
    private PaymentMethodRepository methodRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventRepository eventRepository;
    @Mock
    private PaymentWebhookService webhookService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private CheckoutRateLimiter rateLimiter;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
                planRepository, methodRepository, paymentRepository, eventRepository,
                webhookService, userRepository, gateway, rateLimiter, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC), 30, 24, 3, "http://localhost:4200/");
    }

    @Test
    @DisplayName("lista los planes activos del gimnasio")
    void listsPlans() {
        when(planRepository.findActiveByGym(GYM)).thenReturn(List.of(plan()));

        var plans = service.plans(GYM);

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).name()).isEqualTo("Mensual");
        assertThat(plans.get(0).priceCents()).isEqualTo(9_900);
        assertThat(plans.get(0).durationDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("lista los metodos de pago con su familia")
    void listsPaymentMethods() {
        when(methodRepository.findEnabledByGym(GYM)).thenReturn(List.of(
                new PaymentMethod("CARD", "Tarjeta", PaymentMethodKind.CARD, true)));

        var methods = service.paymentMethods(GYM);

        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).kind()).isEqualTo("CARD");
        assertThat(methods.get(0).immediate()).isTrue();
    }

    @Test
    @DisplayName("abre el checkout, guarda la referencia y devuelve la URL alojada")
    void createsCheckout() {
        when(planRepository.findActiveById(PLAN, GYM)).thenReturn(Optional.of(plan()));
        when(methodRepository.isEnabled(GYM, "CARD")).thenReturn(true);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user(USER, GYM)));
        when(paymentRepository.countRecentFailures(eq(USER), eq(PLAN), any(Instant.class)))
                .thenReturn(0);
        when(paymentRepository.countAttempts(USER, PLAN)).thenReturn(2);
        when(gateway.name()).thenReturn("SANDBOX");
        when(paymentRepository.insert(
                eq(GYM), eq(USER), eq(PLAN), eq(9_900), eq("COP"), eq("CARD"),
                eq("SANDBOX"), anyString(), eq(3), eq(EXPIRES)))
                .thenReturn(PAYMENT);
        when(gateway.createCheckout(any(CheckoutCommand.class)))
                .thenReturn(new CheckoutSession(
                        "http://localhost:4200/app/checkout/42?sandbox=1", EXPIRES));
        when(paymentRepository.findById(PAYMENT)).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        CheckoutResponse response = service.createCheckout(
                USER, GYM, new CheckoutRequest(PLAN, " card "));

        assertThat(response.paymentId()).isEqualTo(PAYMENT);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.amountCents()).isEqualTo(9_900);
        assertThat(response.checkoutUrl()).contains("sandbox=1");
        assertThat(response.expiresAt()).isEqualTo(EXPIRES);
        verify(rateLimiter).check(USER);
        verify(paymentRepository).attachCheckout(
                PAYMENT, "http://localhost:4200/app/checkout/42?sandbox=1");
    }

    @Test
    @DisplayName("envia a la pasarela el comando con los datos del plan y del usuario")
    void sendsGatewayCommand() {
        when(planRepository.findActiveById(PLAN, GYM)).thenReturn(Optional.of(plan()));
        when(methodRepository.isEnabled(GYM, "CARD")).thenReturn(true);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user(USER, GYM)));
        when(paymentRepository.countRecentFailures(eq(USER), eq(PLAN), any(Instant.class)))
                .thenReturn(0);
        when(paymentRepository.countAttempts(USER, PLAN)).thenReturn(0);
        when(gateway.name()).thenReturn("SANDBOX");
        when(paymentRepository.insert(
                eq(GYM), eq(USER), eq(PLAN), eq(9_900), eq("COP"), eq("CARD"),
                eq("SANDBOX"), anyString(), eq(1), eq(EXPIRES)))
                .thenReturn(PAYMENT);
        when(gateway.createCheckout(any(CheckoutCommand.class)))
                .thenReturn(new CheckoutSession("http://checkout", EXPIRES));
        when(paymentRepository.findById(PAYMENT)).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        service.createCheckout(USER, GYM, new CheckoutRequest(PLAN, "CARD"));

        ArgumentCaptor<CheckoutCommand> command = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(gateway).createCheckout(command.capture());
        assertThat(command.getValue().amountCents()).isEqualTo(9_900);
        assertThat(command.getValue().planName()).isEqualTo("Mensual");
        assertThat(command.getValue().userEmail()).isEqualTo("ana@powerzone.app");
        assertThat(command.getValue().successUrl()).contains("/app/checkout/42");
        assertThat(command.getValue().expiresAt()).isEqualTo(EXPIRES);
    }

    @Test
    @DisplayName("rechaza un plan inexistente o inactivo")
    void rejectsUnknownPlan() {
        when(planRepository.findActiveById(99L, GYM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCheckout(
                USER, GYM, new CheckoutRequest(99L, "CARD")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PLAN_NOT_FOUND");
                    assertThat(api.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("rechaza un metodo de pago no habilitado para el gimnasio")
    void rejectsDisabledPaymentMethod() {
        when(planRepository.findActiveById(PLAN, GYM)).thenReturn(Optional.of(plan()));
        when(methodRepository.isEnabled(GYM, "TRANSFER")).thenReturn(false);

        assertThatThrownBy(() -> service.createCheckout(
                USER, GYM, new CheckoutRequest(PLAN, "transfer")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_METHOD_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("limita los reintentos tras varios fallos recientes")
    void enforcesRetryLimit() {
        when(planRepository.findActiveById(PLAN, GYM)).thenReturn(Optional.of(plan()));
        when(methodRepository.isEnabled(GYM, "CARD")).thenReturn(true);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user(USER, GYM)));
        when(paymentRepository.countRecentFailures(eq(USER), eq(PLAN), any(Instant.class)))
                .thenReturn(3);

        assertThatThrownBy(() -> service.createCheckout(
                USER, GYM, new CheckoutRequest(PLAN, "CARD")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PAYMENT_RETRY_LIMIT");
                    assertThat(api.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
        verify(paymentRepository, never()).insert(
                anyLong(), anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt(),
                anyString(), anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    @DisplayName("traduce una falla de la pasarela a 502 sin dejar pago huerfano")
    void translatesGatewayFailure() {
        when(planRepository.findActiveById(PLAN, GYM)).thenReturn(Optional.of(plan()));
        when(methodRepository.isEnabled(GYM, "CARD")).thenReturn(true);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user(USER, GYM)));
        when(paymentRepository.countRecentFailures(eq(USER), eq(PLAN), any(Instant.class)))
                .thenReturn(0);
        when(paymentRepository.countAttempts(USER, PLAN)).thenReturn(0);
        when(gateway.name()).thenReturn("SANDBOX");
        when(paymentRepository.insert(
                eq(GYM), eq(USER), eq(PLAN), eq(9_900), eq("COP"), eq("CARD"),
                eq("SANDBOX"), anyString(), eq(1), eq(EXPIRES)))
                .thenReturn(PAYMENT);
        when(gateway.createCheckout(any(CheckoutCommand.class)))
                .thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> service.createCheckout(
                USER, GYM, new CheckoutRequest(PLAN, "CARD")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PAYMENT_GATEWAY_ERROR");
                    assertThat(api.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @Test
    @DisplayName("devuelve el historial paginado del usuario autenticado")
    void returnsOwnHistory() {
        when(paymentRepository.findHistory(USER, GYM, null, null, null, 10, 0))
                .thenReturn(List.of(payment(PaymentStatus.COMPLETED)));
        when(paymentRepository.countHistory(USER, GYM, null, null, null)).thenReturn(12L);

        PageResponse<PaymentResponse> page = service.history(
                USER, GYM, false, null, null, null, null, 0, 10);

        assertThat(page.items()).hasSize(1);
        assertThat(page.total()).isEqualTo(12);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items().get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("normaliza estado y acota pagina y tamano")
    void sanitizesPagingAndStatus() {
        when(paymentRepository.findHistory(USER, GYM, null, null, "PENDING", 50, 0))
                .thenReturn(List.of());
        when(paymentRepository.countHistory(USER, GYM, null, null, "PENDING")).thenReturn(0L);

        PageResponse<PaymentResponse> page = service.history(
                USER, GYM, false, null, null, null, "pending", -4, 500);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(50);
        assertThat(page.totalPages()).isZero();
        verify(paymentRepository).findHistory(USER, GYM, null, null, "PENDING", 50, 0);
    }

    @Test
    @DisplayName("filtra por rango de fechas")
    void filtersByDateRange() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(paymentRepository.findHistory(USER, GYM, from, to, "COMPLETED", 10, 0))
                .thenReturn(List.of(payment(PaymentStatus.COMPLETED)));
        when(paymentRepository.countHistory(USER, GYM, from, to, "COMPLETED")).thenReturn(1L);

        PageResponse<PaymentResponse> page = service.history(
                USER, GYM, false, null, from, to, "COMPLETED", 0, 10);

        assertThat(page.total()).isEqualTo(1);
        verify(paymentRepository).findHistory(USER, GYM, from, to, "COMPLETED", 10, 0);
    }

    @Test
    @DisplayName("rechaza un estado de pago desconocido")
    void rejectsInvalidStatus() {
        assertThatThrownBy(() -> service.history(
                USER, GYM, false, null, null, null, "pagado", 0, 10))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_PAYMENT_STATUS"));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("rechaza un rango de fechas invertido")
    void rejectsInvertedDateRange() {
        LocalDate from = LocalDate.of(2026, 11, 30);
        LocalDate to = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> service.history(
                USER, GYM, false, null, from, to, null, 0, 10))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("un miembro no puede consultar el historial de otro usuario")
    void forbidsForeignHistory() {
        assertThatThrownBy(() -> service.history(
                USER, GYM, false, 99L, null, null, null, 0, 10))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PAYMENT_FORBIDDEN");
                    assertThat(api.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("el staff puede consultar a un miembro de su propio gimnasio")
    void allowsStaffFromSameGym() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, GYM)));
        when(paymentRepository.findHistory(99L, GYM, null, null, null, 10, 0))
                .thenReturn(List.of());

        PageResponse<PaymentResponse> page = service.history(
                USER, GYM, true, 99L, null, null, null, 0, 10);

        assertThat(page.total()).isZero();
        verify(paymentRepository).findHistory(99L, GYM, null, null, null, 10, 0);
    }

    @Test
    @DisplayName("el staff de otro gimnasio no puede ver pagos ajenos")
    void forbidsStaffFromOtherGym() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, 2L)));

        assertThatThrownBy(() -> service.history(
                USER, GYM, true, 99L, null, null, null, 0, 10))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_FORBIDDEN"));
    }

    @Test
    @DisplayName("el dueño consulta el detalle de su pago")
    void returnsOwnPayment() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        PaymentResponse response = service.payment(PAYMENT, USER, GYM, false);

        assertThat(response.id()).isEqualTo(PAYMENT);
        assertThat(response.reference()).isEqualTo("pz_abc123");
    }

    @Test
    @DisplayName("el staff del mismo gimnasio puede consultar el detalle")
    void allowsStaffPaymentDetail() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));

        assertThat(service.payment(PAYMENT, 99L, GYM, true).id()).isEqualTo(PAYMENT);
    }

    @Test
    @DisplayName("otros usuarios no pueden ver el detalle de un pago")
    void forbidsForeignPaymentDetail() {
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.payment(PAYMENT, 99L, GYM, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_FORBIDDEN"));
    }

    @Test
    @DisplayName("el sandbox solo esta disponible con la pasarela simulada")
    void rejectsSandboxWithRealGateway() {
        when(gateway.supportsSandbox()).thenReturn(false);

        assertThatThrownBy(() -> service.sandboxPay(USER, PAYMENT))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("SANDBOX_DISABLED");
                    assertThat(api.status()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("el sandbox exige que el pago exista y sea del usuario")
    void validatesSandboxOwnership() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sandboxPay(USER, PAYMENT))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("el sandbox rechaza pagos de otro usuario")
    void forbidsForeignSandboxPayment() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.sandboxPay(99L, PAYMENT))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_FORBIDDEN"));
    }

    @Test
    @DisplayName("el sandbox rechaza pagos ya procesados")
    void rejectsSandboxForResolvedPayment() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));

        assertThatThrownBy(() -> service.sandboxPay(USER, PAYMENT))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("PAYMENT_NOT_PENDING"));
    }

    @Test
    @DisplayName("simula un pago aprobado por el mismo camino del webhook")
    void simulatesApprovedPayment() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)))
                .thenReturn(Optional.of(payment(PaymentStatus.COMPLETED)));
        when(gateway.sign(anyString(), anyString())).thenReturn("v1=signed");
        when(gateway.verify(anyString(), eq("v1=signed"), anyString()))
                .thenReturn(WebhookVerification.VALID);
        when(webhookService.process(any(WebhookEvent.class)))
                .thenReturn(new WebhookResponse(true, false, "COMPLETED"));

        PaymentResponse response = service.sandboxPay(USER, PAYMENT);

        assertThat(response.status()).isEqualTo("COMPLETED");
        ArgumentCaptor<WebhookEvent> event = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookService).process(event.capture());
        assertThat(event.getValue().id()).startsWith("evt_sbx_");
        assertThat(event.getValue().type()).isEqualTo("payment.completed");
        assertThat(event.getValue().data().reference()).isEqualTo("pz_abc123");
        assertThat(event.getValue().data().status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("simula un rechazo con el codigo indicado")
    void simulatesRejectedPayment() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)))
                .thenReturn(Optional.of(payment(PaymentStatus.FAILED)));
        when(gateway.sign(anyString(), anyString())).thenReturn("v1=signed");
        when(gateway.verify(anyString(), eq("v1=signed"), anyString()))
                .thenReturn(WebhookVerification.VALID);
        when(webhookService.process(any(WebhookEvent.class)))
                .thenReturn(new WebhookResponse(true, false, "FAILED"));

        PaymentResponse response = service.sandboxFail(USER, PAYMENT, "expired_card");

        assertThat(response.status()).isEqualTo("FAILED");
        ArgumentCaptor<WebhookEvent> event = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookService).process(event.capture());
        assertThat(event.getValue().type()).isEqualTo("payment.failed");
        assertThat(event.getValue().data().failureCode()).isEqualTo("EXPIRED_CARD");
    }

    @Test
    @DisplayName("un rechazo simulado sin codigo usa CARD_DECLINED")
    void defaultsSandboxFailureCode() {
        when(gateway.supportsSandbox()).thenReturn(true);
        when(paymentRepository.findById(PAYMENT))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)))
                .thenReturn(Optional.of(payment(PaymentStatus.FAILED)));
        when(gateway.sign(anyString(), anyString())).thenReturn("v1=signed");
        when(gateway.verify(anyString(), eq("v1=signed"), anyString()))
                .thenReturn(WebhookVerification.VALID);
        when(webhookService.process(any(WebhookEvent.class)))
                .thenReturn(new WebhookResponse(true, false, "FAILED"));

        service.sandboxFail(USER, PAYMENT, null);

        ArgumentCaptor<WebhookEvent> event = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookService).process(event.capture());
        assertThat(event.getValue().data().failureCode()).isEqualTo("CARD_DECLINED");
    }

    @Test
    @DisplayName("rechaza un webhook vacio antes de verificar la firma")
    void rejectsEmptyWebhook() {
        assertThatThrownBy(() -> service.handleWebhook("  ", "v1=x", "1"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("EMPTY_WEBHOOK"));
        verifyNoInteractions(gateway, webhookService);
    }

    @Test
    @DisplayName("rechaza una firma invalida y deja bitacora")
    void rejectsInvalidSignature() {
        when(gateway.verify("{}", "v1=bad", "1"))
                .thenReturn(WebhookVerification.INVALID_SIGNATURE);

        assertThatThrownBy(() -> service.handleWebhook("{}", "v1=bad", "1"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("INVALID_SIGNATURE");
                    assertThat(api.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(eventRepository).recordRejected(anyString(), eq("REJECTED_INVALID_SIGNATURE"));
        verifyNoInteractions(webhookService);
    }

    @Test
    @DisplayName("reporta un timestamp vencido al cliente")
    void reportsStaleTimestamp() {
        when(gateway.verify("{}", "v1=old", "1"))
                .thenReturn(WebhookVerification.STALE_TIMESTAMP);

        assertThatThrownBy(() -> service.handleWebhook("{}", "v1=old", "1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("antiguo");
    }

    @Test
    @DisplayName("rechaza un payload con JSON invalido")
    void rejectsInvalidJson() {
        when(gateway.verify("no-json", "v1=ok", "1"))
                .thenReturn(WebhookVerification.VALID);

        assertThatThrownBy(() -> service.handleWebhook("no-json", "v1=ok", "1"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_WEBHOOK_PAYLOAD"));
    }

    @Test
    @DisplayName("rechaza un evento sin los campos minimos")
    void rejectsIncompletePayload() {
        String payload = "{\"id\":\"evt_1\",\"type\":\"payment.completed\",\"data\":{}}";
        when(gateway.verify(payload, "v1=ok", "1"))
                .thenReturn(WebhookVerification.VALID);

        assertThatThrownBy(() -> service.handleWebhook(payload, "v1=ok", "1"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_WEBHOOK_PAYLOAD"));
        verifyNoInteractions(webhookService);
    }

    @Test
    @DisplayName("un webhook valido se delega al servicio transaccional")
    void delegatesValidWebhook() {
        String payload = """
                {"id":"evt_1","type":"payment.completed",
                 "data":{"reference":"pz_abc123","status":"COMPLETED"}}
                """;
        when(gateway.verify(payload, "v1=ok", "1"))
                .thenReturn(WebhookVerification.VALID);
        when(webhookService.process(any(WebhookEvent.class)))
                .thenReturn(new WebhookResponse(true, false, "COMPLETED"));

        WebhookResponse response = service.handleWebhook(payload, "v1=ok", "1");

        assertThat(response.outcome()).isEqualTo("COMPLETED");
        ArgumentCaptor<WebhookEvent> event = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookService).process(event.capture());
        assertThat(event.getValue().id()).isEqualTo("evt_1");
        assertThat(event.getValue().data().reference()).isEqualTo("pz_abc123");
    }

    private static Plan plan() {
        return new Plan(PLAN, GYM, "MONTHLY", "Mensual", "Acceso mensual",
                9_900, "COP", 30, true);
    }

    private static UserAccount user(long id, long gymId) {
        return new UserAccount(
                id, gymId, "ana@powerzone.app", "hash", "Ana", "Gomez", "3001234567",
                "MEMBER", true, LocalDate.of(1998, 3, 14), null, null,
                "STRENGTH", "INTERMEDIATE", 4);
    }

    private static Payment payment(PaymentStatus status) {
        return new Payment(
                PAYMENT, GYM, USER, PLAN, "Mensual", 9_900, "COP",
                "CARD", "Tarjeta", status.name(), "SANDBOX", "pz_abc123",
                "http://localhost:4200/app/checkout/42?sandbox=1", null, null, 1,
                EXPIRES, status == PaymentStatus.COMPLETED ? NOW : null,
                NOW.minusSeconds(60));
    }
}
