package app.powerzone.progress;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProgressRepository {

    private static final RowMapper<ProgressEntry> MAPPER = (rs, rowNum) -> new ProgressEntry(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("exercise_id"),
            rs.getString("exercise_name"),
            nullableLong(rs.getObject("routine_exercise_id")),
            date(rs.getDate("performed_on")),
            rs.getBigDecimal("weight_kg"),
            nullableInteger(rs.getObject("reps")),
            nullableInteger(rs.getObject("sets")),
            rs.getString("notes"),
            instant(rs.getTimestamp("created_at")));

    private static final RowMapper<ProgressSummary> SUMMARY_MAPPER = (rs, rowNum) -> new ProgressSummary(
            rs.getLong("exercise_id"),
            rs.getString("exercise_name"),
            rs.getInt("sessions"),
            rs.getBigDecimal("max_weight"),
            rs.getBigDecimal("last_weight"),
            date(rs.getDate("last_performed")));

    private final JdbcTemplate jdbc;

    public ProgressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long userId,
                       long exerciseId,
                       Long routineExerciseId,
                       LocalDate performedOn,
                       BigDecimal weightKg,
                       Integer reps,
                       Integer sets,
                       String notes) {
        Long id = jdbc.query("""
                INSERT INTO exercise_progress (
                    user_id, exercise_id, routine_exercise_id, performed_on,
                    weight_kg, reps, sets, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId, exerciseId, routineExerciseId, performedOn,
                weightKg, reps, sets, notes);
        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el id del progreso insertado");
        }
        return id;
    }

    public Optional<ProgressEntry> findById(long id) {
        return jdbc.query("""
                SELECT p.*, x.name AS exercise_name
                  FROM exercise_progress p
                  JOIN exercises x ON x.id = p.exercise_id
                 WHERE p.id = ?
                """, MAPPER, id).stream().findFirst();
    }

    /** Historial del usuario, con filtros opcionales por ejercicio y fechas. */
    public List<ProgressEntry> findByUser(long userId,
                                          Long exerciseId,
                                          LocalDate from,
                                          LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.*, x.name AS exercise_name
                  FROM exercise_progress p
                  JOIN exercises x ON x.id = p.exercise_id
                 WHERE p.user_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (exerciseId != null) {
            sql.append(" AND p.exercise_id = ?");
            params.add(exerciseId);
        }
        if (from != null) {
            sql.append(" AND p.performed_on >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND p.performed_on <= ?");
            params.add(Date.valueOf(to));
        }
        sql.append(" ORDER BY p.performed_on DESC, p.id DESC");

        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    /**
     * Resumen por ejercicio: sesiones, mejor carga, ultima carga y ultima
     * fecha. `array_agg` devuelve la carga mas reciente sin una subconsulta.
     */
    public List<ProgressSummary> summaryByUser(long userId) {
        return jdbc.query("""
                SELECT p.exercise_id,
                       x.name AS exercise_name,
                       COUNT(*) AS sessions,
                       MAX(p.weight_kg) AS max_weight,
                       (array_agg(p.weight_kg ORDER BY p.performed_on DESC, p.id DESC))[1] AS last_weight,
                       MAX(p.performed_on) AS last_performed
                  FROM exercise_progress p
                  JOIN exercises x ON x.id = p.exercise_id
                 WHERE p.user_id = ?
                 GROUP BY p.exercise_id, x.name
                 ORDER BY MAX(p.performed_on) DESC, x.name
                """, SUMMARY_MAPPER, userId);
    }

    public int countByUser(long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercise_progress WHERE user_id = ?",
                Integer.class, userId);
        return count == null ? 0 : count;
    }

    public void delete(long id, long userId) {
        jdbc.update("DELETE FROM exercise_progress WHERE id = ? AND user_id = ?", id, userId);
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static LocalDate date(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
