package app.powerzone.routines;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Persistencia de rutinas con JDBC explicito. La jerarquia se escribe en tres
 * inserts (rutina, dia, ejercicio) y se lee con una consulta por nivel.
 */
@Repository
public class RoutineRepository {

    private static final RowMapper<Routine> ROUTINE_MAPPER = (rs, rowNum) -> new Routine(
            rs.getLong("id"),
            rs.getLong("gym_id"),
            rs.getLong("user_id"),
            rs.getString("name"),
            rs.getString("goal"),
            rs.getString("experience_level"),
            rs.getInt("days_per_week"),
            rs.getString("status"),
            rs.getString("source"),
            instant(rs.getTimestamp("created_at")),
            instant(rs.getTimestamp("updated_at")));

    private static final RowMapper<DayRow> DAY_MAPPER = (rs, rowNum) -> new DayRow(
            rs.getLong("id"),
            rs.getInt("day_number"),
            rs.getString("name"),
            rs.getString("focus"));

    private static final RowMapper<ExerciseRow> EXERCISE_MAPPER = (rs, rowNum) -> new ExerciseRow(
            rs.getLong("day_id"),
            new RoutineExercise(
                    rs.getLong("id"),
                    rs.getLong("exercise_id"),
                    rs.getString("exercise_name"),
                    rs.getString("muscle_group"),
                    rs.getString("equipment"),
                    rs.getInt("position"),
                    rs.getInt("sets"),
                    rs.getString("reps"),
                    rs.getInt("rest_seconds"),
                    rs.getString("notes")));

    private final JdbcTemplate jdbc;

    public RoutineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertRoutine(long gymId,
                              long userId,
                              String name,
                              String goal,
                              String experienceLevel,
                              int daysPerWeek,
                              String source) {
        Long id = jdbc.query("""
                INSERT INTO routines (
                    gym_id, user_id, name, goal, experience_level, days_per_week, status, source
                ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                RETURNING id
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                gymId, userId, name, goal, experienceLevel, daysPerWeek, source);
        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el id de la rutina insertada");
        }
        return id;
    }

    public long insertDay(long routineId, int dayNumber, String name, String focus) {
        Long id = jdbc.query("""
                INSERT INTO routine_days (routine_id, day_number, name, focus)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                routineId, dayNumber, name, focus);
        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el id del dia insertado");
        }
        return id;
    }

    public void insertExercise(long routineDayId,
                               long exerciseId,
                               int position,
                               int sets,
                               String reps,
                               int restSeconds,
                               String notes) {
        jdbc.update("""
                INSERT INTO routine_exercises (
                    routine_day_id, exercise_id, position, sets, reps, rest_seconds, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                routineDayId, exerciseId, position, sets, reps, restSeconds, notes);
    }

    public Optional<Routine> findById(long routineId) {
        return jdbc.query(
                "SELECT * FROM routines WHERE id = ?", ROUTINE_MAPPER, routineId)
                .stream().findFirst();
    }

    public Optional<Routine> findByIdAndUser(long routineId, long userId) {
        return jdbc.query(
                "SELECT * FROM routines WHERE id = ? AND user_id = ?",
                ROUTINE_MAPPER, routineId, userId).stream().findFirst();
    }

    public List<Routine> findByUser(long userId) {
        return jdbc.query("""
                SELECT * FROM routines
                 WHERE user_id = ?
                 ORDER BY created_at DESC, id DESC
                """, ROUTINE_MAPPER, userId);
    }

    public List<Routine> findActiveByUser(long userId) {
        return jdbc.query("""
                SELECT * FROM routines
                 WHERE user_id = ? AND status = 'ACTIVE'
                 ORDER BY created_at DESC, id DESC
                """, ROUTINE_MAPPER, userId);
    }

    /** Carga la rutina con dias y ejercicios en tres consultas. */
    public Optional<RoutineDetail> findDetail(long routineId) {
        Optional<Routine> routine = findById(routineId);
        if (routine.isEmpty()) {
            return Optional.empty();
        }

        List<DayRow> days = jdbc.query("""
                SELECT id, day_number, name, focus
                  FROM routine_days
                 WHERE routine_id = ?
                 ORDER BY day_number
                """, DAY_MAPPER, routineId);

        List<ExerciseRow> exercises = jdbc.query("""
                SELECT d.id AS day_id,
                       e.id, e.exercise_id, x.name AS exercise_name,
                       x.muscle_group, x.equipment, e.position, e.sets, e.reps,
                       e.rest_seconds, e.notes
                  FROM routine_exercises e
                  JOIN routine_days d ON d.id = e.routine_day_id
                  JOIN exercises x ON x.id = e.exercise_id
                 WHERE d.routine_id = ?
                 ORDER BY d.day_number, e.position
                """, EXERCISE_MAPPER, routineId);

        Map<Long, List<RoutineExercise>> byDay = new LinkedHashMap<>();
        for (ExerciseRow row : exercises) {
            byDay.computeIfAbsent(row.dayId(), key -> new ArrayList<>()).add(row.exercise());
        }

        List<RoutineDay> detailDays = days.stream()
                .map(day -> new RoutineDay(
                        day.id(),
                        day.dayNumber(),
                        day.name(),
                        day.focus(),
                        byDay.getOrDefault(day.id(), List.of())))
                .toList();

        return Optional.of(new RoutineDetail(routine.get(), detailDays));
    }

    public Optional<RoutineDay> findDay(long routineId, int dayNumber) {
        return jdbc.query("""
                SELECT id, day_number, name, focus
                  FROM routine_days
                 WHERE routine_id = ? AND day_number = ?
                """, DAY_MAPPER, routineId, dayNumber)
                .stream()
                .findFirst()
                .map(day -> new RoutineDay(
                        day.id(),
                        day.dayNumber(),
                        day.name(),
                        day.focus(),
                        jdbc.query("""
                                SELECT e.id, e.exercise_id, x.name AS exercise_name,
                                       x.muscle_group, x.equipment, e.position, e.sets,
                                       e.reps, e.rest_seconds, e.notes
                                  FROM routine_exercises e
                                  JOIN exercises x ON x.id = e.exercise_id
                                 WHERE e.routine_day_id = ?
                                 ORDER BY e.position
                                """, EXERCISE_MAPPER, day.id())
                                .stream()
                                .map(ExerciseRow::exercise)
                                .toList()));
    }

    public Optional<RoutineExercise> findRoutineExercise(long routineExerciseId) {
        return jdbc.query("""
                SELECT d.id AS day_id, e.id, e.exercise_id, x.name AS exercise_name,
                       x.muscle_group, x.equipment, e.position, e.sets, e.reps,
                       e.rest_seconds, e.notes
                  FROM routine_exercises e
                  JOIN routine_days d ON d.id = e.routine_day_id
                  JOIN exercises x ON x.id = e.exercise_id
                 WHERE e.id = ?
                """, EXERCISE_MAPPER, routineExerciseId)
                .stream()
                .findFirst()
                .map(ExerciseRow::exercise);
    }

    /** Comprueba que el ejercicio prescrito pertenece a una rutina del usuario. */
    public boolean belongsToUser(long routineExerciseId, long userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM routine_exercises e
                  JOIN routine_days d ON d.id = e.routine_day_id
                  JOIN routines r ON r.id = d.routine_id
                 WHERE e.id = ? AND r.user_id = ?
                """, Integer.class, routineExerciseId, userId);
        return count != null && count > 0;
    }

    /** Comprueba que el ejercicio prescrito pertenece a la rutina indicada. */
    public boolean belongsToRoutine(long routineExerciseId, long routineId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM routine_exercises e
                  JOIN routine_days d ON d.id = e.routine_day_id
                 WHERE e.id = ? AND d.routine_id = ?
                """, Integer.class, routineExerciseId, routineId);
        return count != null && count > 0;
    }

    /** Reemplaza la prescripcion de un ejercicio (entrenador o administrador). */
    public void updatePrescription(long routineExerciseId,
                                   int sets,
                                   String reps,
                                   int restSeconds,
                                   String notes) {
        jdbc.update("""
                UPDATE routine_exercises
                   SET sets = ?, reps = ?, rest_seconds = ?, notes = ?
                 WHERE id = ?
                """, sets, reps, restSeconds, notes, routineExerciseId);
    }

    public void updateStatus(long routineId, String status) {
        jdbc.update("UPDATE routines SET status = ? WHERE id = ?", status, routineId);
    }

    public void delete(long routineId) {
        jdbc.update("DELETE FROM routines WHERE id = ?", routineId);
    }

    public int countByUser(long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM routines WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record DayRow(long id, int dayNumber, String name, String focus) {
    }

    private record ExerciseRow(long dayId, RoutineExercise exercise) {
    }
}
