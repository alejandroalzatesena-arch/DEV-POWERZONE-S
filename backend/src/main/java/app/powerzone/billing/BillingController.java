package app.powerzone.billing;

import app.powerzone.billing.dto.CheckoutRequest;
import app.powerzone.billing.dto.CheckoutResponse;
import app.powerzone.billing.dto.MembershipResponse;
import app.powerzone.billing.dto.PageResponse;
import app.powerzone.billing.dto.PaymentMethodResponse;
import app.powerzone.billing.dto.PaymentResponse;
import app.powerzone.billing.dto.PlanResponse;
import app.powerzone.billing.dto.SandboxFailureRequest;
import app.powerzone.billing.receipt.ReceiptDocument;
import app.powerzone.billing.receipt.ReceiptService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final PaymentService paymentService;
    private final MembershipService membershipService;
    private final ReceiptService receiptService;

    public BillingController(PaymentService paymentService,
                             MembershipService membershipService,
                             ReceiptService receiptService) {
        this.paymentService = paymentService;
        this.membershipService = membershipService;
        this.receiptService = receiptService;
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans(@AuthenticationPrincipal Jwt jwt) {
        return paymentService.plans(gymId(jwt));
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> paymentMethods(@AuthenticationPrincipal Jwt jwt) {
        return paymentService.paymentMethods(gymId(jwt));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = paymentService.createCheckout(
                subjectId(jwt), gymId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/billing/payments/" + response.paymentId()))
                .body(response);
    }

    @GetMapping("/payments")
    public PageResponse<PaymentResponse> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return paymentService.history(
                subjectId(jwt), gymId(jwt), staff(jwt), userId, from, to, status, page, size);
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentResponse payment(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable long paymentId) {
        return paymentService.payment(
                paymentId, subjectId(jwt), gymId(jwt), staff(jwt));
    }

    @GetMapping("/membership")
    public MembershipResponse membership(@AuthenticationPrincipal Jwt jwt) {
        return membershipService.status(gymId(jwt), subjectId(jwt));
    }

    @GetMapping("/payments/{paymentId}/receipt")
    public ResponseEntity<byte[]> receipt(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable long paymentId) {
        ReceiptDocument document = receiptService.receipt(
                paymentId, subjectId(jwt), gymId(jwt), staff(jwt));
        String filename = "comprobante-" + document.number() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentLength(document.content().length)
                .body(document.content());
    }

    @PostMapping("/payments/{paymentId}/sandbox/pay")
    public PaymentResponse sandboxPay(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable long paymentId) {
        return paymentService.sandboxPay(subjectId(jwt), paymentId);
    }

    @PostMapping("/payments/{paymentId}/sandbox/fail")
    public PaymentResponse sandboxFail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long paymentId,
            @RequestBody(required = false) SandboxFailureRequest request) {
        String code = request == null ? null : request.failureCode();
        return paymentService.sandboxFail(subjectId(jwt), paymentId, code);
    }

    static long subjectId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    static long gymId(Jwt jwt) {
        Object claim = jwt.getClaim("gymId");
        return claim instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(claim));
    }

    static boolean staff(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("ADMIN") || roles.contains("TRAINER")
                || roles.contains("SUPER_ADMIN"));
    }
}
