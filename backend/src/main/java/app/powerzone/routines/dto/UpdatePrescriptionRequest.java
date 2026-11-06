package app.powerzone.routines.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ajuste de la prescripcion por parte de un entrenador o administrador. */
public record UpdatePrescriptionRequest(
        @Min(value = 1, message = "Las series deben ser al menos 1")
        @Max(value = 10, message = "Las series no pueden superar 10")
        int sets,

        @NotBlank(message = "Las repeticiones son obligatorias")
        @Pattern(regexp = "^\\d{1,2}(-\\d{1,2})?$",
                message = "Las repeticiones deben tener el formato 8 o 8-12")
        String reps,

        @Min(value = 10, message = "El descanso minimo es de 10 segundos")
        @Max(value = 600, message = "El descanso maximo es de 600 segundos")
        int restSeconds,

        @Size(max = 200, message = "La nota no puede superar 200 caracteres")
        String notes) {
}
