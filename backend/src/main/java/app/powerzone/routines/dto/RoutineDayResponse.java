package app.powerzone.routines.dto;

import app.powerzone.routines.RoutineDay;
import java.util.List;

/** Un dia de rutina con sus ejercicios. */
public record RoutineDayResponse(
        long id,
        int dayNumber,
        String name,
        String focus,
        List<Exercise> exercises) {

    public static RoutineDayResponse from(RoutineDay day) {
        return new RoutineDayResponse(
                day.id(),
                day.dayNumber(),
                day.name(),
                day.focus(),
                day.exercises().stream().map(Exercise::from).toList());
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

        static Exercise from(app.powerzone.routines.RoutineExercise exercise) {
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
