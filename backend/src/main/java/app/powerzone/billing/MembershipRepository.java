package app.powerzone.billing;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipRepository {

    private static final RowMapper<Membership> MAPPER = (rs, rowNum) -> new Membership(
            rs.getLong("id"),
            rs.getLong("gym_id"),
            rs.getLong("user_id"),
            rs.getLong("plan_id"),
            rs.getString("plan_name"),
            rs.getObject("starts_at", LocalDate.class),
            rs.getObject("ends_at", LocalDate.class),
            rs.getTimestamp("updated_at") == null
                    ? null
                    : rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbc;

    public MembershipRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Membership> findByUser(long gymId, long userId) {
        return jdbc.query("""
                SELECT m.id, m.gym_id, m.user_id, m.plan_id, p.name AS plan_name,
                       m.starts_at, m.ends_at, m.updated_at
                  FROM memberships m
                  JOIN membership_plans p ON p.id = m.plan_id
                 WHERE m.gym_id = ? AND m.user_id = ?
                """, MAPPER, gymId, userId).stream().findFirst();
    }

    public void upsert(long gymId,
                       long userId,
                       long planId,
                       LocalDate startsAt,
                       LocalDate endsAt) {
        jdbc.update("""
                INSERT INTO memberships (gym_id, user_id, plan_id, starts_at, ends_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (gym_id, user_id)
                DO UPDATE SET plan_id = EXCLUDED.plan_id,
                              starts_at = EXCLUDED.starts_at,
                              ends_at = EXCLUDED.ends_at,
                              updated_at = now()
                """, gymId, userId, planId, Date.valueOf(startsAt), Date.valueOf(endsAt));
    }

    public Instant lastUpdate(long gymId, long userId) {
        return jdbc.query("""
                SELECT updated_at FROM memberships WHERE gym_id = ? AND user_id = ?
                """, rs -> rs.next() ? rs.getTimestamp("updated_at").toInstant() : null,
                gymId, userId);
    }
}
