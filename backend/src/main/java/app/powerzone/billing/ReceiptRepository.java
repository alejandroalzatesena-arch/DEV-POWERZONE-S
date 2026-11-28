package app.powerzone.billing;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Numeracion consecutiva de comprobantes, uno por pago completado. */
@Repository
public class ReceiptRepository {

    private static final RowMapper<Receipt> MAPPER = (rs, rowNum) -> new Receipt(
            rs.getLong("payment_id"),
            rs.getLong("number"),
            rs.getTimestamp("issued_at").toInstant());

    private final JdbcTemplate jdbc;

    public ReceiptRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Emite el comprobante una sola vez. Si el pago ya tenia uno, devuelve el
     * existente para que la descarga sea idempotente.
     */
    public Receipt issue(long paymentId) {
        jdbc.update("""
                INSERT INTO payment_receipts (payment_id)
                VALUES (?)
                ON CONFLICT (payment_id) DO NOTHING
                """, paymentId);
        return find(paymentId).orElseThrow(() -> new IllegalStateException(
                "No se pudo emitir el comprobante del pago " + paymentId));
    }

    public Optional<Receipt> find(long paymentId) {
        return jdbc.query("""
                SELECT payment_id, number, issued_at
                  FROM payment_receipts
                 WHERE payment_id = ?
                """, MAPPER, paymentId).stream().findFirst();
    }
}
