package app.powerzone.billing;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Metodos de pago habilitados por gimnasio. */
@Repository
public class PaymentMethodRepository {

    private static final RowMapper<PaymentMethod> MAPPER = (rs, rowNum) -> new PaymentMethod(
            rs.getString("code"),
            rs.getString("label"),
            PaymentMethodKind.valueOf(rs.getString("kind")),
            rs.getBoolean("immediate"));

    private final JdbcTemplate jdbc;

    public PaymentMethodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PaymentMethod> findEnabledByGym(long gymId) {
        return jdbc.query("""
                SELECT m.code, m.label, m.kind, m.immediate
                  FROM payment_methods m
                  JOIN gym_payment_methods g ON g.method_code = m.code
                 WHERE g.gym_id = ? AND g.enabled = TRUE AND m.active = TRUE
                 ORDER BY m.kind, m.label
                """, MAPPER, gymId);
    }

    public boolean isEnabled(long gymId, String methodCode) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM payment_methods m
                  JOIN gym_payment_methods g ON g.method_code = m.code
                 WHERE g.gym_id = ? AND m.code = ? AND g.enabled = TRUE AND m.active = TRUE
                """, Integer.class, gymId, methodCode);
        return count != null && count > 0;
    }
}
