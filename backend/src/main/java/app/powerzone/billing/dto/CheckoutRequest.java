package app.powerzone.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull(message = "El plan es obligatorio")
        Long planId,
        @NotBlank(message = "El metodo de pago es obligatorio")
        @Size(max = 30)
        String methodCode) {
}
