package app.powerzone.routines.dto;

import app.powerzone.routines.RoutineDetail;
import app.powerzone.routines.RoutineExercise;
import java.time.Instant;
import java.util.List;

/** Rutina con dias y ejercicios. */
public record RoutineDetailResponse(
        long id,
        String name,
        String goal,
        String experienceLevel,
        int daysPerWeek,
        String status,
        String source,
        Instant createdAt,
        List<Day> days) {

    public static RoutineDetailResponse from(RoutineDetail detail) {
        return new RoutineDetailResponse(
                detail.routine().id(),
                detail.routine().name(),
                detail.routine().goal(),
                detail.routine().experienceLevel(),
                detail.routine().daysPerWeek(),
                detail.routine().status(),
                detail.routine().source(),
                detail.routine().createdAt(),
                detail.days().stream().map(Day::from).toList());
    }

    public record Day(
            long id,
            int dayNumber,
            String name,
            String focus,
            List<Exercise> exercises) {

        static Day from(app.powerzone.routines.RoutineDay day) {
            return new Day(
                    day.id(),
                    day.dayNumber(),
                    day.name(),
                    day.focus(),
                    day.exercises().stream().map(Exercise::from).toList());
        }
    }

    public record Exercise(
            long id,
            long exerciseId,
            String name,
            String muscleGroup,
            String equipment,
            int position,
            int sets,
            String reps,
            int restSeconds,
            String notes) {

        static Exercise from(RoutineExercise exercise) {
            return new Exercise(
                    exercise.id(),
                    exercise.exerciseId(),
                    exercise.exerciseName(),
                    exercise.muscleGroup(),
                    exercise.equipment(),
                    exercise.position(),
                    exercise.sets(),
                    exercise.reps(),
                    exercise.restSeconds(),
                    exercise.notes());
        }
    }
}
