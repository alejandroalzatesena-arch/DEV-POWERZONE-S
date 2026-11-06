package app.powerzone.routines.dto;

import app.powerzone.routines.RoutineDetail;
import java.util.List;

/** Vista semanal: solo los dias y sus ejercicios. */
public record RoutineWeekResponse(
        long routineId,
        String name,
        int daysPerWeek,
        List<RoutineDetailResponse.Day> days) {

    public static RoutineWeekResponse from(RoutineDetail detail) {
        return new RoutineWeekResponse(
                detail.routine().id(),
                detail.routine().name(),
                detail.routine().daysPerWeek(),
                detail.days().stream().map(RoutineDetailResponse.Day::from).toList());
    }
}
