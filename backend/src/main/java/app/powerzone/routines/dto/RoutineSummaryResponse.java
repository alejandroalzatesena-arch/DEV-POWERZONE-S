package app.powerzone.routines.dto;

import app.powerzone.routines.Routine;
import java.time.Instant;

/** Cabecera de rutina para listados. */
public record RoutineSummaryResponse(
        long id,
        String name,
        String goal,
        String experienceLevel,
        int daysPerWeek,
        String status,
        String source,
        Instant createdAt) {

    public static RoutineSummaryResponse from(Routine routine) {
        return new RoutineSummaryResponse(
                routine.id(),
                routine.name(),
                routine.goal(),
                routine.experienceLevel(),
                routine.daysPerWeek(),
                routine.status(),
                routine.source(),
                routine.createdAt());
    }
}
