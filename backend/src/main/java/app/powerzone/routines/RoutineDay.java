package app.powerzone.routines;

import java.util.List;

/** Dia de entrenamiento con sus ejercicios ordenados por posicion. */
public record RoutineDay(
        long id,
        int dayNumber,
        String name,
        String focus,
        List<RoutineExercise> exercises) {
}
