package app.powerzone.routines;

import java.time.Instant;

/**
 * Cabecera de una rutina. El detalle de dias y ejercicios se carga aparte para
 * no traer la jerarquia completa en los listados.
 */
public record Routine(
        long id,
        long gymId,
        long userId,
        String name,
        String goal,
        String experienceLevel,
        int daysPerWeek,
        String status,
        String source,
        Instant createdAt,
        Instant updatedAt) {
}
