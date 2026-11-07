package app.powerzone.progress;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Registro de una serie o sesion realizada sobre un ejercicio. */
public record ProgressEntry(
        long id,
        long userId,
        long exerciseId,
        String exerciseName,
        Long routineExerciseId,
        LocalDate performedOn,
        BigDecimal weightKg,
        Integer reps,
        Integer sets,
        String notes,
        Instant createdAt) {
}
