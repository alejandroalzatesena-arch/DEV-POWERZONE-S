package app.powerzone.billing.dto;

import jakarta.validation.constraints.Size;

/** Codigo opcional para simular un rechazo concreto en el sandbox. */
public record SandboxFailureRequest(
        @Size(max = 40) String failureCode) {
}
