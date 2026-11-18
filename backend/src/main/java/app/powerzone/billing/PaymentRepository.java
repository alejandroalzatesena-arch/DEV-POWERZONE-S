package app.powerzone.billing;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Persistencia de pagos. Las fechas se comparan en UTC para que el filtro por
 * rango no dependa de la zona horaria configurada en el servidor.
 */
@Repository
public class PaymentRepository {

    private static final String SELECT = """
            SELECT p.id, p.gym_id, p.user_id, p.plan_id, pl.name AS plan_name,
                   p.amount_cents, p.currency, p.method_code, m.label AS method_label,
                   p.status, p.gateway, p.external_reference, p.checkout_url,
                   p.failure_code, p.failure_message, p.attempt, p.expires_at,
                   p.completed_at, p.created_at
              FROM payments p
              JOIN membership_plans pl ON pl.id = p.plan_id
              JOIN payment_methods m ON m.code = p.method_code
            """;

    private static final RowMapper<Payment> MAPPER = (rs, rowNum) -> new Payment(
            rs.getLong("id"),
            rs.getLong("gym_id"),
            rs.getLong("user_id"),
            rs.getLong("plan_id"),
            rs.getString("plan_name"),
            rs.getInt("amount_cents"),
            rs.getString("currency"),
            rs.getString("method_code"),
            rs.getString("method_label"),
            rs.getString("status"),
            rs.getString("gateway"),
            rs.getString("external_reference"),
            rs.getString("checkout_url"),
            rs.getString("failure_code"),
            rs.getString("failure_message"),
            rs.getInt("attempt"),
            instant(rs.getTimestamp("expires_at")),
            instant(rs.getTimestamp("completed_at")),
            instant(rs.getTimestamp("created_at")));

    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long gymId,
                       long userId,
                       long planId,
                       int amountCents,
                       String currency,
                       String methodCode,
                       String gateway,
                       String reference,
                       int attempt,
                       Instant expiresAt) {
        Long id = jdbc.query("""
                INSERT INTO payments (
                    gym_id, user_id, plan_id, amount_cents, currency, method_code,
                    status, gateway, external_reference, attempt, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?, ?)
                RETURNING id
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                gymId, userId, planId, amountCents, currency, methodCode,
                gateway, reference, attempt, Timestamp.from(expiresAt));
        if (id == null) {
            throw new IllegalStateException("No se pudo obtener el id del pago insertado");
        }
        return id;
    }

    public void attachCheckout(long paymentId, String checkoutUrl) {
        jdbc.update(
                "UPDATE payments SET checkout_url = ? WHERE id = ?",
                checkoutUrl, paymentId);
    }

    public Optional<Payment> findById(long paymentId) {
        return jdbc.query(SELECT + " WHERE p.id = ?", MAPPER, paymentId)
                .stream().findFirst();
    }

    public Optional<Payment> findByReference(String reference) {
        return jdbc.query(SELECT + " WHERE p.external_reference = ?", MAPPER, reference)
                .stream().findFirst();
    }

    public int countAttempts(long userId, long planId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payments WHERE user_id = ? AND plan_id = ?
                """, Integer.class, userId, planId);
        return count == null ? 0 : count;
    }

    public int countRecentFailures(long userId, long planId, Instant since) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM payments
                 WHERE user_id = ? AND plan_id = ?
                   AND status IN ('FAILED', 'EXPIRED')
                   AND created_at >= ?
                """, Integer.class, userId, planId, Timestamp.from(since));
        return count == null ? 0 : count;
    }

    public List<Payment> findHistory(long userId,
                                     long gymId,
                                     LocalDate from,
                                     LocalDate to,
                                     String status,
                                     int size,
                                     int page) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE p.user_id = ? AND p.gym_id = ?");
        appendFilters(sql, args, from, to, status);
        sql.append(" ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?");
        args.add(size);
        args.add((long) page * size);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public long countHistory(long userId,
                             long gymId,
                             LocalDate from,
                             LocalDate to,
                             String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM payments p WHERE p.user_id = ? AND p.gym_id = ?");
        appendFilters(sql, args, from, to, status);
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public int markCompleted(long paymentId, Instant completedAt) {
        return jdbc.update("""
                UPDATE payments
                   SET status = 'COMPLETED', completed_at = ?, failure_code = NULL,
                       failure_message = NULL
                 WHERE id = ? AND status = 'PENDING'
                """, Timestamp.from(completedAt), paymentId);
    }

    public int markFailed(long paymentId,
                          String status,
                          String failureCode,
                          String failureMessage,
                          Instant at) {
        return jdbc.update("""
                UPDATE payments
                   SET status = ?, failure_code = ?, failure_message = ?, completed_at = ?
                 WHERE id = ? AND status = 'PENDING'
                """, status, failureCode, failureMessage, Timestamp.from(at), paymentId);
    }

    public List<Long> findExpiredPending(Instant now, int limit) {
        return jdbc.queryForList("""
                SELECT id
                  FROM payments
                 WHERE status = 'PENDING' AND expires_at <= ?
                 ORDER BY expires_at
                 LIMIT ?
                """, Long.class, Timestamp.from(now), limit);
    }

    private static void appendFilters(StringBuilder sql,
                                      List<Object> args,
                                      LocalDate from,
                                      LocalDate to,
                                      String status) {
        if (from != null) {
            sql.append(" AND p.created_at >= ?");
            args.add(Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (to != null) {
            sql.append(" AND p.created_at < ?");
            args.add(Timestamp.from(
                    to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND p.status = ?");
            args.add(status);
        }
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
