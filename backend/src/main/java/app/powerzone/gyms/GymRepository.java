package app.powerzone.gyms;

import app.powerzone.common.ApiException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la raiz del tenant. En esta etapa la API opera sobre el gimnasio
 * semilla; el selector por cabecera se incorpora con la administracion.
 */
@Repository
public class GymRepository {

    private final JdbcTemplate jdbc;

    public GymRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long defaultGymId() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM gyms WHERE active = TRUE ORDER BY id LIMIT 1", Long.class);
        if (ids.isEmpty()) {
            throw ApiException.notFound("GYM_NOT_FOUND", "No hay un gimnasio activo configurado.");
        }
        return ids.get(0);
    }

    public boolean exists(long gymId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM gyms WHERE id = ? AND active = TRUE", Integer.class, gymId);
        return count != null && count > 0;
    }
}
