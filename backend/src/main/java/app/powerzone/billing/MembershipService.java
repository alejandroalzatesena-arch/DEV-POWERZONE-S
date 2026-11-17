package app.powerzone.billing;

import app.powerzone.billing.dto.MembershipResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula el estado de la membresia con la fecha UTC y activa o extiende la
 * vigencia cuando un pago se completa.
 */
@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final Clock clock;
    private final int expiringDays;

    public MembershipService(MembershipRepository membershipRepository,
                             Clock clock,
                             @Value("${powerzone.payments.expiring-days:7}") int expiringDays) {
        this.membershipRepository = membershipRepository;
        this.clock = clock;
        this.expiringDays = expiringDays;
    }

    @Transactional(readOnly = true)
    public MembershipResponse status(long gymId, long userId) {
        LocalDate today = LocalDate.now(clock);
        Optional<Membership> found = membershipRepository.findByUser(gymId, userId);
        if (found.isEmpty()) {
            return new MembershipResponse(
                    MembershipState.NONE, null, null, null, 0, expiringDays);
        }

        Membership membership = found.get();
        if (!membership.endsAt().isAfter(today)) {
            return new MembershipResponse(
                    MembershipState.EXPIRED, membership.planName(), membership.startsAt(),
                    membership.endsAt(), 0, expiringDays);
        }

        long days = ChronoUnit.DAYS.between(today, membership.endsAt());
        MembershipState state = days <= expiringDays
                ? MembershipState.EXPIRING
                : MembershipState.ACTIVE;
        return new MembershipResponse(
                state, membership.planName(), membership.startsAt(), membership.endsAt(),
                days, expiringDays);
    }

    /**
     * Si la membresia sigue vigente, el nuevo periodo arranca al terminar el
     * actual; si ya vencio o no existe, arranca hoy.
     */
    @Transactional
    public Membership activateOrRenew(long gymId,
                                      long userId,
                                      long planId,
                                      int durationDays) {
        LocalDate today = LocalDate.now(clock);
        Optional<Membership> current = membershipRepository.findByUser(gymId, userId);
        LocalDate startsAt = current
                .filter(membership -> membership.endsAt().isAfter(today))
                .map(Membership::endsAt)
                .orElse(today);
        LocalDate endsAt = startsAt.plusDays(durationDays);
        membershipRepository.upsert(gymId, userId, planId, startsAt, endsAt);
        return membershipRepository.findByUser(gymId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo recuperar la membresia actualizada"));
    }

    public int expiringDays() {
        return expiringDays;
    }
}
