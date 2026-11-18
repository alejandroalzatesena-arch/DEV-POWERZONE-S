package app.powerzone.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Catalogo de planes de membresia del gimnasio. */
@Repository
public class PlanRepository {

    private static final RowMapper<Plan> MAPPER = (rs, rowNum) -> new Plan(
            rs.getLong("id"),
            rs.getLong("gym_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("price_cents"),
            rs.getString("currency"),
            rs.getInt("duration_days"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Plan> findActiveByGym(long gymId) {
        return jdbc.query("""
                SELECT id, gym_id, code, name, description, price_cents, currency,
                       duration_days, active
                  FROM membership_plans
                 WHERE gym_id = ? AND active = TRUE
                 ORDER BY duration_days, price_cents
                """, MAPPER, gymId);
    }

    public Optional<Plan> findActiveById(long planId, long gymId) {
        return jdbc.query("""
                SELECT id, gym_id, code, name, description, price_cents, currency,
                       duration_days, active
                  FROM membership_plans
                 WHERE id = ? AND gym_id = ? AND active = TRUE
                """, MAPPER, planId, gymId).stream().findFirst();
    }
}
