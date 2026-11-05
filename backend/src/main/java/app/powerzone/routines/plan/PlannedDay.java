package app.powerzone.routines.plan;

import java.util.List;

public record PlannedDay(
        int dayNumber,
        String name,
        String focus,
        List<PlannedExercise> exercises) {
}
