package app.powerzone.routines;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ExerciseRepository {

    private static final String SELECT = """
            SELECT id, name, muscle_group, equipment, difficulty, description, active
              FROM exercises
            """;

    private static final RowMapper<Exercise> MAPPER = (rs, rowNum) -> new Exercise(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("muscle_group"),
            rs.getString("equipment"),
            rs.getString("difficulty"),
            rs.getString("description"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;

    public ExerciseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Exercise> findAllActive() {
        return jdbc.query(SELECT + " WHERE active = TRUE ORDER BY muscle_group, name", MAPPER);
    }

    public Optional<Exercise> findById(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<Exercise> findByMuscleGroup(String muscleGroup) {
        return jdbc.query(
                SELECT + " WHERE active = TRUE AND muscle_group = ? ORDER BY difficulty, name",
                MAPPER, muscleGroup);
    }

    /**
     * Catalogo agrupado por grupo muscular. El generador elige ejercicios con
     * esta vista para evitar una consulta por grupo.
     */
    public List<Exercise> findActiveByGroups(Collection<String> muscleGroups) {
        if (muscleGroups == null || muscleGroups.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(muscleGroups.size(), "?"));
        return jdbc.query(
                SELECT + " WHERE active = TRUE AND muscle_group IN (" + placeholders + ")"
                        + " ORDER BY muscle_group, difficulty, name",
                MAPPER, muscleGroups.toArray());
    }

    public int countActive() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercises WHERE active = TRUE", Integer.class);
        return count == null ? 0 : count;
    }
}
