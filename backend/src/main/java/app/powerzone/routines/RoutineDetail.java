package app.powerzone.routines;

import java.util.List;

/** Rutina con la jerarquia completa de dias y ejercicios. */
public record RoutineDetail(Routine routine, List<RoutineDay> days) {
}
