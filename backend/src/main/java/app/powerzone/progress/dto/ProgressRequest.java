package app.powerzone.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de progreso. Al menos una metrica debe venir informada. */
public record ProgressRequest(
        @NotNull(message = "El ejercicio es obligatorio")
        Long exerciseId,

        Long routineExerciseId,

        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate performedOn,

        @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
        @DecimalMax(value = "500.0", message = "El peso no puede superar 500 kg")
        BigDecimal weightKg,

        @Min(value = 1, message = "Las repeticiones deben ser al menos 1")
        @Max(value = 100, message = "Las repeticiones no pueden superar 100")
        Integer reps,

        @Min(value = 1, message = "Las series deben ser al menos 1")
        @Max(value = 10, message = "Las series no pueden superar 10")
        Integer sets,

        @Size(max = 200, message = "La nota no puede superar 200 caracteres")
        String notes) {
}
