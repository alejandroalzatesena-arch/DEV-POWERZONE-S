package app.powerzone.billing;

import java.time.Instant;
import java.time.LocalDate;

/** Membresia vigente de un usuario en un gimnasio. */
public record Membership(
        long id,
        long gymId,
        long userId,
        long planId,
        String planName,
        LocalDate startsAt,
        LocalDate endsAt,
        Instant updatedAt) {
}
