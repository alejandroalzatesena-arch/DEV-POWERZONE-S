package app.powerzone.progress.dto;

import app.powerzone.progress.ProgressEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProgressResponse(
        long id,
        long exerciseId,
        String exerciseName,
        Long routineExerciseId,
        LocalDate performedOn,
        BigDecimal weightKg,
        Integer reps,
        Integer sets,
        String notes,
        Instant createdAt) {

    public static ProgressResponse from(ProgressEntry entry) {
        return new ProgressResponse(
                entry.id(),
                entry.exerciseId(),
                entry.exerciseName(),
                entry.routineExerciseId(),
                entry.performedOn(),
                entry.weightKg(),
                entry.reps(),
                entry.sets(),
                entry.notes(),
                entry.createdAt());
    }
}
