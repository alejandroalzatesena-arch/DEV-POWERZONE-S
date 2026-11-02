package app.powerzone.routines;

/**
 * Ejercicio del catalogo. El campo {@code muscleGroup} usa los valores del
 * check de la tabla: CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, FULL_BODY y
 * CARDIO.
 */
public record Exercise(
        long id,
        String name,
        String muscleGroup,
        String equipment,
        String difficulty,
        String description,
        boolean active) {
}
