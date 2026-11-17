package app.powerzone.billing.receipt;

import java.time.Instant;

public record ReceiptData(
        String gymName,
        String userName,
        String userEmail,
        long number,
        Instant issuedAt,
        String planName,
        int amountCents,
        String currency,
        String methodLabel,
        String status) {
}
