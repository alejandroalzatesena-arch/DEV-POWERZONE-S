package app.powerzone.billing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bitacora de webhooks. El alta usa ON CONFLICT para que dos entregas
 * simultaneas del mismo evento solo procesen una.
 */
@Repository
public class PaymentEventRepository {

    private final JdbcTemplate jdbc;

    public PaymentEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean tryInsert(String eventId, String eventType) {
        int inserted = jdbc.update("""
                INSERT INTO payment_events (event_id, event_type, signature_valid, outcome)
                VALUES (?, ?, TRUE, 'RECEIVED')
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType);
        return inserted > 0;
    }

    public void finish(String eventId, Long paymentId, String outcome) {
        jdbc.update("""
                UPDATE payment_events
                   SET payment_id = ?, outcome = ?, processed_at = now()
                 WHERE event_id = ?
                """, paymentId, outcome, eventId);
    }

    public void recordRejected(String eventId, String outcome) {
        jdbc.update("""
                INSERT INTO payment_events (event_id, event_type, signature_valid, outcome)
                VALUES (?, 'UNKNOWN', FALSE, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, outcome);
    }
}
