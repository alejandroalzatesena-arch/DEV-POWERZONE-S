package app.powerzone.users;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Proyeccion de un usuario con el codigo de rol resuelto. No es una entidad
 * JPA: el acceso a datos es explicito con JDBC.
 */
public record UserAccount(
        long id,
        long gymId,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String phone,
        String role,
        boolean active,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String goal,
        String experienceLevel,
        Integer daysPerWeek) {
}
