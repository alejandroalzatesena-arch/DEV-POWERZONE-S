package app.powerzone.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepository {

    private static final RowMapper<PasswordResetRow> MAPPER = (rs, rowNum) -> new PasswordResetRow(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getTimestamp("expires_at").toInstant());

    private final JdbcTemplate jdbc;

    public PasswordResetTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(long userId, String tokenHash, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """, userId, tokenHash, java.sql.Timestamp.from(expiresAt));
    }

    public Optional<PasswordResetRow> findValidByHash(String tokenHash) {
        List<PasswordResetRow> rows = jdbc.query("""
                SELECT id, user_id, expires_at
                  FROM password_reset_tokens
                 WHERE token_hash = ?
                   AND used = FALSE
                   AND expires_at > now()
                """, MAPPER, tokenHash);
        return rows.stream().findFirst();
    }

    public void markUsed(long id) {
        jdbc.update("UPDATE password_reset_tokens SET used = TRUE WHERE id = ?", id);
    }

    public void invalidateForUser(long userId) {
        jdbc.update("UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND used = FALSE", userId);
    }

    public int deleteExpired() {
        return jdbc.update("DELETE FROM password_reset_tokens WHERE expires_at < now() - interval '1 day'");
    }

    public record PasswordResetRow(long id, long userId, Instant expiresAt) {
    }
}
