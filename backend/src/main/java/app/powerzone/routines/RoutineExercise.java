package app.powerzone.routines;

/**
 * Ejercicio prescrito dentro de un dia de rutina. Incluye los datos del
 * catalogo que necesita la interfaz para renderizar la sesion sin consultas
 * adicionales.
 */
public record RoutineExercise(
        long id,
        long exerciseId,
        String exerciseName,
        String muscleGroup,
        String equipment,
        int position,
        int sets,
        String reps,
        int restSeconds,
        String notes) {
}
