package app.powerzone.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    private static final RowMapper<RefreshTokenRow> MAPPER = (rs, rowNum) -> new RefreshTokenRow(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getBoolean("revoked"));

    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(long userId, String tokenHash, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """, userId, tokenHash, java.sql.Timestamp.from(expiresAt));
    }

    public Optional<RefreshTokenRow> findValidByHash(String tokenHash) {
        List<RefreshTokenRow> rows = jdbc.query("""
                SELECT id, user_id, expires_at, revoked
                  FROM refresh_tokens
                 WHERE token_hash = ?
                   AND revoked = FALSE
                   AND expires_at > now()
                """, MAPPER, tokenHash);
        return rows.stream().findFirst();
    }

    public Optional<RefreshTokenRow> findByHash(String tokenHash) {
        List<RefreshTokenRow> rows = jdbc.query("""
                SELECT id, user_id, expires_at, revoked
                  FROM refresh_tokens
                 WHERE token_hash = ?
                """, MAPPER, tokenHash);
        return rows.stream().findFirst();
    }

    public void revoke(long id) {
        jdbc.update("UPDATE refresh_tokens SET revoked = TRUE WHERE id = ?", id);
    }

    public void revokeAllForUser(long userId) {
        jdbc.update("UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = ?", userId);
    }

    public int deleteExpired() {
        return jdbc.update("DELETE FROM refresh_tokens WHERE expires_at < now() - interval '1 day'");
    }

    public int activeSessions(long userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM refresh_tokens
                 WHERE user_id = ? AND revoked = FALSE AND expires_at > now()
                """, Integer.class, userId);
        return count == null ? 0 : count;
    }

    /** Revoca las sesiones mas antiguas para conservar solo las mas recientes. */
    public void keepMostRecent(long userId, int keep) {
        jdbc.update("""
                UPDATE refresh_tokens
                   SET revoked = TRUE
                 WHERE user_id = ?
                   AND revoked = FALSE
                   AND id NOT IN (
                       SELECT id FROM refresh_tokens
                        WHERE user_id = ? AND revoked = FALSE
                        ORDER BY created_at DESC
                        LIMIT ?
                   )
                """, userId, userId, keep);
    }

    public record RefreshTokenRow(long id, long userId, Instant expiresAt, boolean revoked) {
    }
}
