package app.powerzone.billing.receipt;

import app.powerzone.billing.Payment;
import app.powerzone.billing.PaymentRepository;
import app.powerzone.billing.PaymentStatus;
import app.powerzone.billing.Receipt;
import app.powerzone.billing.ReceiptRepository;
import app.powerzone.common.ApiException;
import app.powerzone.gyms.GymRepository;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite y renderiza el comprobante de un pago completado. La autorizacion se
 * resuelve antes de generar el PDF.
 */
@Service
public class ReceiptService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final ReceiptPdfRenderer renderer;

    public ReceiptService(PaymentRepository paymentRepository,
                          ReceiptRepository receiptRepository,
                          UserRepository userRepository,
                          GymRepository gymRepository,
                          ReceiptPdfRenderer renderer) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.gymRepository = gymRepository;
        this.renderer = renderer;
    }

    @Transactional
    public ReceiptDocument receipt(long paymentId,
                                   long requesterId,
                                   long requesterGymId,
                                   boolean staff) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(
                        "PAYMENT_NOT_FOUND", "No se encontro el pago solicitado."));

        if (payment.userId() != requesterId
                && (!staff || payment.gymId() != requesterGymId)) {
            throw ApiException.forbidden(
                    "RECEIPT_FORBIDDEN", "No tienes acceso a este comprobante.");
        }
        if (payment.statusValue() != PaymentStatus.COMPLETED) {
            throw ApiException.conflict(
                    "RECEIPT_NOT_AVAILABLE",
                    "Solo los pagos completados tienen comprobante.");
        }

        Receipt receipt = receiptRepository.issue(payment.id());
        UserAccount user = userRepository.findById(payment.userId())
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario del pago."));
        String gymName = gymRepository.findName(payment.gymId()).orElse("PowerZone");

        byte[] pdf = renderer.render(new ReceiptData(
                gymName,
                user.firstName() + " " + user.lastName(),
                user.email(),
                receipt.number(),
                receipt.issuedAt(),
                payment.planName(),
                payment.amountCents(),
                payment.currency(),
                payment.methodLabel(),
                payment.status()));

        return new ReceiptDocument(pdf, receipt.number());
    }
}
