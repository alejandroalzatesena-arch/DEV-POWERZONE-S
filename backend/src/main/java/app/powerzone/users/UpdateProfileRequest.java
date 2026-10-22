package app.powerzone.users;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos editables del perfil. El email y el rol no se modifican desde aqui. */
public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String goal,
        String experienceLevel,
        Integer daysPerWeek) {
}
