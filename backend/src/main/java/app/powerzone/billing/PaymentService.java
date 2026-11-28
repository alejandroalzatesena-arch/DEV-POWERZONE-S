package app.powerzone.billing;

import app.powerzone.billing.dto.CheckoutRequest;
import app.powerzone.billing.dto.CheckoutResponse;
import app.powerzone.billing.dto.PageResponse;
import app.powerzone.billing.dto.PaymentMethodResponse;
import app.powerzone.billing.dto.PaymentResponse;
import app.powerzone.billing.dto.PlanResponse;
import app.powerzone.billing.dto.WebhookEvent;
import app.powerzone.billing.dto.WebhookResponse;
import app.powerzone.billing.gateway.CheckoutCommand;
import app.powerzone.billing.gateway.CheckoutSession;
import app.powerzone.billing.gateway.PaymentGateway;
import app.powerzone.billing.gateway.WebhookVerification;
import app.powerzone.common.ApiException;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta el ciclo de vida del pago: apertura del checkout, consulta,
 * historial, webhook firmado y simulacion del sandbox. Nunca recibe datos de
 * tarjeta; solo referencias opacas de la pasarela.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final PlanRepository planRepository;
    private final PaymentMethodRepository methodRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final PaymentWebhookService webhookService;
    private final UserRepository userRepository;
    private final PaymentGateway gateway;
    private final CheckoutRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int checkoutMinutes;
    private final int retryWindowHours;
    private final int maxAttempts;
    private final String webUrl;

    public PaymentService(PlanRepository planRepository,
                          PaymentMethodRepository methodRepository,
                          PaymentRepository paymentRepository,
                          PaymentEventRepository eventRepository,
                          PaymentWebhookService webhookService,
                          UserRepository userRepository,
                          PaymentGateway gateway,
                          CheckoutRateLimiter rateLimiter,
                          ObjectMapper objectMapper,
                          Clock clock,
                          @Value("${powerzone.payments.checkout-minutes:30}") int checkoutMinutes,
                          @Value("${powerzone.payments.retry-window-hours:24}") int retryWindowHours,
                          @Value("${powerzone.payments.max-attempts:3}") int maxAttempts,
                          @Value("${powerzone.web-url:http://localhost:4200}") String webUrl) {
        this.planRepository = planRepository;
        this.methodRepository = methodRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.webhookService = webhookService;
        this.userRepository = userRepository;
        this.gateway = gateway;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.checkoutMinutes = checkoutMinutes;
        this.retryWindowHours = retryWindowHours;
        this.maxAttempts = maxAttempts;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> plans(long gymId) {
        return planRepository.findActiveByGym(gymId).stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> paymentMethods(long gymId) {
        return methodRepository.findEnabledByGym(gymId).stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    @Transactional
    public CheckoutResponse createCheckout(long userId,
                                           long gymId,
                                           CheckoutRequest request) {
        rateLimiter.check(userId);

        Plan plan = planRepository.findActiveById(request.planId(), gymId)
                .orElseThrow(() -> ApiException.notFound(
                        "PLAN_NOT_FOUND", "El plan solicitado no esta disponible."));

        String methodCode = request.methodCode().trim().toUpperCase(Locale.ROOT);
        if (!methodRepository.isEnabled(gymId, methodCode)) {
            throw ApiException.badRequest(
                    "PAYMENT_METHOD_NOT_AVAILABLE",
                    "El metodo de pago no esta habilitado para este gimnasio.");
        }

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario de la sesion."));

        Instant now = clock.instant();
        int recentFailures = paymentRepository.countRecentFailures(
                userId, plan.id(), now.minusSeconds(retryWindowHours * 3600L));
        if (recentFailures >= maxAttempts) {
            throw ApiException.tooManyRequests(
                    "PAYMENT_RETRY_LIMIT",
                    "Alcanzaste el limite de intentos fallidos. Espera "
                            + retryWindowHours + " horas o contacta al gimnasio.");
        }

        int attempt = paymentRepository.countAttempts(userId, plan.id()) + 1;
        String reference = "pz_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = now.plusSeconds(checkoutMinutes * 60L);
        long paymentId = paymentRepository.insert(
                gymId, userId, plan.id(), plan.priceCents(), plan.currency(),
                methodCode, gateway.name(), reference, attempt, expiresAt);

        String successUrl = webUrl + "/app/checkout/" + paymentId;
        String cancelUrl = webUrl + "/app/plans";
        try {
            CheckoutSession session = gateway.createCheckout(new CheckoutCommand(
                    paymentId, reference, plan.priceCents(), plan.currency(), plan.name(),
                    methodCode, user.email(), successUrl, cancelUrl, expiresAt));
            paymentRepository.attachCheckout(paymentId, session.checkoutUrl());
        } catch (RuntimeException ex) {
            // La transaccion se revierte: no queda un pago huerfano en la base.
            log.warn("No se pudo abrir el checkout del pago {}: {}", paymentId, ex.getMessage());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_GATEWAY_ERROR",
                    "No se pudo iniciar el pago. Intenta de nuevo en unos minutos.");
        }

        return CheckoutResponse.from(load(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse payment(long paymentId,
                                   long requesterId,
                                   long requesterGymId,
                                   boolean staff) {
        return PaymentResponse.from(
                loadAuthorized(paymentId, requesterId, requesterGymId, staff));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> history(long requesterId,
                                                 long requesterGymId,
                                                 boolean staff,
                                                 Long userId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 String status,
                                                 int page,
                                                 int size) {
        long targetUserId = userId == null ? requesterId : userId;
        if (targetUserId != requesterId) {
            if (!staff) {
                throw ApiException.forbidden(
                        "PAYMENT_FORBIDDEN",
                        "No puedes consultar los pagos de otro usuario.");
            }
            UserAccount target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> ApiException.notFound(
                            "USER_NOT_FOUND", "No se encontro el usuario solicitado."));
            if (target.gymId() != requesterGymId) {
                throw ApiException.forbidden(
                        "PAYMENT_FORBIDDEN", "El usuario pertenece a otro gimnasio.");
            }
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest(
                    "INVALID_DATE_RANGE",
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }

        String normalizedStatus = normalizeStatus(status);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<Payment> payments = paymentRepository.findHistory(
                targetUserId, requesterGymId, from, to, normalizedStatus, safeSize, safePage);
        long total = paymentRepository.countHistory(
                targetUserId, requesterGymId, from, to, normalizedStatus);

        return PageResponse.of(
                payments.stream().map(PaymentResponse::from).toList(),
                safePage, safeSize, total);
    }

    /**
     * Verifica la firma y delega el cambio de estado al servicio transaccional.
     * Un evento repetido responde 2xx para que la pasarela deje de reintentar.
     */
    public WebhookResponse handleWebhook(String payload,
                                         String signature,
                                         String timestamp) {
        if (payload == null || payload.isBlank()) {
            throw ApiException.badRequest(
                    "EMPTY_WEBHOOK", "El webhook no contiene un cuerpo valido.");
        }

        WebhookVerification verification = gateway.verify(payload, signature, timestamp);
        if (verification != WebhookVerification.VALID) {
            eventRepository.recordRejected(
                    "rejected:" + sha256(payload), "REJECTED_" + verification.name());
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SIGNATURE",
                    switch (verification) {
                        case MISSING_SIGNATURE -> "El webhook no incluye la firma.";
                        case STALE_TIMESTAMP -> "El webhook es demasiado antiguo.";
                        default -> "La firma del webhook no es valida.";
                    });
        }

        WebhookEvent event;
        try {
            event = objectMapper.readValue(payload, WebhookEvent.class);
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest(
                    "INVALID_WEBHOOK_PAYLOAD", "El webhook no tiene el formato esperado.");
        }

        if (event.id() == null || event.id().isBlank()
                || event.type() == null || event.type().isBlank()
                || event.data() == null || event.data().reference() == null
                || event.data().reference().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_WEBHOOK_PAYLOAD", "El webhook no tiene el formato esperado.");
        }

        return webhookService.process(event);
    }

    /**
     * Simula la confirmacion de la pasarela para el entorno de desarrollo.
     * Genera un webhook firmado y lo procesa por el mismo camino que uno real.
     */
    public PaymentResponse sandboxPay(long userId, long paymentId) {
        Payment payment = requireSandboxPayment(userId, paymentId);
        String eventId = "evt_sbx_" + UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        String payload = toJson(Map.of(
                "id", eventId,
                "type", "payment.completed",
                "data", Map.of(
                        "reference", payment.reference(),
                        "status", "COMPLETED")));
        handleWebhook(payload, gateway.sign(payload, timestamp), timestamp);
        return PaymentResponse.from(load(paymentId));
    }

    public PaymentResponse sandboxFail(long userId, long paymentId, String failureCode) {
        Payment payment = requireSandboxPayment(userId, paymentId);
        String code = failureCode == null || failureCode.isBlank()
                ? "CARD_DECLINED"
                : failureCode.trim().toUpperCase(Locale.ROOT);
        String eventId = "evt_sbx_" + UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        String payload = toJson(Map.of(
                "id", eventId,
                "type", "payment.failed",
                "data", Map.of(
                        "reference", payment.reference(),
                        "status", "FAILED",
                        "failureCode", code)));
        handleWebhook(payload, gateway.sign(payload, timestamp), timestamp);
        return PaymentResponse.from(load(paymentId));
    }

    private Payment requireSandboxPayment(long userId, long paymentId) {
        if (!gateway.supportsSandbox()) {
            throw ApiException.conflict(
                    "SANDBOX_DISABLED",
                    "La simulacion de pagos solo esta disponible con la pasarela de desarrollo.");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(
                        "PAYMENT_NOT_FOUND", "No se encontro el pago solicitado."));
        if (payment.userId() != userId) {
            throw ApiException.forbidden(
                    "PAYMENT_FORBIDDEN", "El pago pertenece a otro usuario.");
        }
        if (payment.statusValue() != PaymentStatus.PENDING) {
            throw ApiException.conflict(
                    "PAYMENT_NOT_PENDING", "El pago ya fue procesado.");
        }
        return payment;
    }

    private Payment load(long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(
                        "PAYMENT_NOT_FOUND", "No se encontro el pago solicitado."));
    }

    private Payment loadAuthorized(long paymentId,
                                   long requesterId,
                                   long requesterGymId,
                                   boolean staff) {
        Payment payment = load(paymentId);
        if (payment.userId() == requesterId) {
            return payment;
        }
        if (!staff || payment.gymId() != requesterGymId) {
            throw ApiException.forbidden(
                    "PAYMENT_FORBIDDEN", "No tienes acceso a este pago.");
        }
        return payment;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(
                    "INVALID_PAYMENT_STATUS", "El estado de pago indicado no es valido.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el webhook simulado", ex);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
