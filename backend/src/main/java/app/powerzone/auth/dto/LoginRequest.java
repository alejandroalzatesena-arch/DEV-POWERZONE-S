package app.powerzone.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 160, message = "El email no puede superar 160 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 72, message = "La contrasena no puede superar 72 caracteres")
        String password,

        boolean rememberMe) {
}
