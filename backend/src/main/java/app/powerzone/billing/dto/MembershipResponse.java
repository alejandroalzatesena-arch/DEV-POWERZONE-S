package app.powerzone.billing.dto;

import app.powerzone.billing.MembershipState;
import java.time.LocalDate;

public record MembershipResponse(
        MembershipState state,
        String planName,
        LocalDate startsAt,
        LocalDate endsAt,
        long daysRemaining,
        int expiringThresholdDays) {
}
