package app.powerzone.users;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String SELECT_USER = """
            SELECT u.id, u.gym_id, u.email, u.password_hash, u.first_name, u.last_name,
                   u.phone, u.birth_date, u.height_cm, u.weight_kg, u.goal,
                   u.experience_level, u.days_per_week, u.active, r.code AS role_code
              FROM users u
              JOIN roles r ON r.id = u.role_id
            """;

    private static final RowMapper<UserAccount> MAPPER = (rs, rowNum) -> {
        int days = rs.getInt("days_per_week");
        Integer daysPerWeek = rs.wasNull() ? null : days;
        return new UserAccount(
                rs.getLong("id"),
                rs.getLong("gym_id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("role_code"),
                rs.getBoolean("active"),
                rs.getObject("birth_date", LocalDate.class),
                rs.getBigDecimal("height_cm"),
                rs.getBigDecimal("weight_kg"),
                rs.getString("goal"),
                rs.getString("experience_level"),
                daysPerWeek);
    };

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByGymAndEmail(long gymId, String email) {
        List<UserAccount> users = jdbc.query(
                SELECT_USER + " WHERE u.gym_id = ? AND LOWER(u.email) = LOWER(?)",
                MAPPER, gymId, email);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findById(long id) {
        List<UserAccount> users = jdbc.query(SELECT_USER + " WHERE u.id = ?", MAPPER, id);
        return users.stream().findFirst();
    }

    public List<UserAccount> findAllByGym(long gymId) {
        return jdbc.query(
                SELECT_USER + " WHERE u.gym_id = ? ORDER BY u.first_name, u.last_name",
                MAPPER, gymId);
    }

    public boolean existsByGymAndEmail(long gymId, String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE gym_id = ? AND LOWER(email) = LOWER(?)",
                Integer.class, gymId, email);
        return count != null && count > 0;
    }

    /**
     * Inserta el usuario y devuelve el identificador generado. Se usa
     * `setObject` para que los campos opcionales nulos conserven su tipo.
     */
    public long insert(long gymId,
                       String roleCode,
                       String email,
                       String passwordHash,
                       String firstName,
                       String lastName,
                       String phone,
                       LocalDate birthDate,
                       BigDecimal heightCm,
                       BigDecimal weightKg,
                       String goal,
                       String experienceLevel,
                       Integer daysPerWeek) {
        String sql = """
                INSERT INTO users (
                    gym_id, role_id, email, password_hash, first_name, last_name, phone,
                    birth_date, height_cm, weight_kg, goal, experience_level, days_per_week
                ) VALUES (
                    ?, (SELECT id FROM roles WHERE code = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                ) RETURNING id
                """;

        Long id = jdbc.query(connection -> {
            var ps = connection.prepareStatement(sql);
            ps.setLong(1, gymId);
            ps.setString(2, roleCode);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setString(5, firstName);
            ps.setString(6, lastName);
            ps.setObject(7, phone);
            ps.setObject(8, birthDate);
            ps.setObject(9, heightCm);
            ps.setObject(10, weightKg);
            ps.setObject(11, goal);
            ps.setObject(12, experienceLevel);
            ps.setObject(13, daysPerWeek);
            return ps;
        }, rs -> rs.next() ? rs.getLong("id") : null);

        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el id del usuario insertado");
        }
        return id;
    }

    public void updateProfile(long userId,
                              String firstName,
                              String lastName,
                              String phone,
                              LocalDate birthDate,
                              BigDecimal heightCm,
                              BigDecimal weightKg,
                              String goal,
                              String experienceLevel,
                              Integer daysPerWeek) {
        jdbc.update("""
                UPDATE users
                   SET first_name = ?, last_name = ?, phone = ?, birth_date = ?,
                       height_cm = ?, weight_kg = ?, goal = ?, experience_level = ?,
                       days_per_week = ?
                 WHERE id = ?
                """,
                firstName, lastName, phone, birthDate, heightCm, weightKg,
                goal, experienceLevel, daysPerWeek, userId);
    }

    public void updatePassword(long userId, String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public void setActive(long userId, boolean active) {
        jdbc.update("UPDATE users SET active = ? WHERE id = ?", active, userId);
    }

    public void updateRole(long userId, String roleCode) {
        jdbc.update("""
                UPDATE users SET role_id = (SELECT id FROM roles WHERE code = ?) WHERE id = ?
                """, roleCode, userId);
    }
}
