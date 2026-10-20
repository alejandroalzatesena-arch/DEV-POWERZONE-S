package app.powerzone.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 160, message = "El email no puede superar 160 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contrasena debe incluir al menos una letra y un numero")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
        String lastName,

        @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
        String phone,

        @Past(message = "La fecha de nacimiento debe estar en el pasado")
        LocalDate birthDate,

        @DecimalMin(value = "50.0", message = "La estatura minima es 50 cm")
        @DecimalMax(value = "250.0", message = "La estatura maxima es 250 cm")
        BigDecimal heightCm,

        @DecimalMin(value = "20.0", message = "El peso minimo es 20 kg")
        @DecimalMax(value = "300.0", message = "El peso maximo es 300 kg")
        BigDecimal weightKg,

        @Pattern(
                regexp = "STRENGTH|HYPERTROPHY|WEIGHT_LOSS|ENDURANCE",
                message = "El objetivo debe ser STRENGTH, HYPERTROPHY, WEIGHT_LOSS o ENDURANCE")
        String goal,

        @Pattern(
                regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
                message = "El nivel debe ser BEGINNER, INTERMEDIATE o ADVANCED")
        String experienceLevel,

        @Min(value = 2, message = "Debes entrenar al menos 2 dias por semana")
        @Max(value = 7, message = "No puedes entrenar mas de 7 dias por semana")
        Integer daysPerWeek) {
}
