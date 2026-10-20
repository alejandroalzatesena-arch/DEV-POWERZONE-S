package app.powerzone.users;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representacion publica del usuario. Nunca expone el hash de la contrasena. */
public record UserResponse(
        long id,
        long gymId,
        String email,
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

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.id(),
                user.gymId(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.phone(),
                user.role(),
                user.active(),
                user.birthDate(),
                user.heightCm(),
                user.weightKg(),
                user.goal(),
                user.experienceLevel(),
                user.daysPerWeek());
    }
}
