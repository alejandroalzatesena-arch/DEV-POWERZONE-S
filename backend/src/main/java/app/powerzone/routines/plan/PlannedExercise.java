package app.powerzone.routines.plan;

public record PlannedExercise(
        long exerciseId,
        String exerciseName,
        String muscleGroup,
        int position,
        int sets,
        String reps,
        int restSeconds,
        String notes) {
}
