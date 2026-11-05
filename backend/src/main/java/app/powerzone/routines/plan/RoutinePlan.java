package app.powerzone.routines.plan;

import java.util.List;

/**
 * Resultado del motor de generacion antes de persistirse. Es inmutable y no
 * conoce la base de datos, lo que permite probar la generacion de forma
 * aislada.
 */
public record RoutinePlan(
        String name,
        String goal,
        String experienceLevel,
        int daysPerWeek,
        List<PlannedDay> days) {
}
