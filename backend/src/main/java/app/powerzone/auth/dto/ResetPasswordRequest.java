package app.powerzone.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contrasena debe incluir al menos una letra y un numero")
        String newPassword) {
}
