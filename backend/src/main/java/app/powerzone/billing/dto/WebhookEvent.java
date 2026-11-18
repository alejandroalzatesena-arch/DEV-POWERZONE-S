package app.powerzone.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento minimo que consume el webhook. Los campos desconocidos se ignoran
 * para que la pasarela pueda evolucionar sin romper la integracion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEvent(String id, String type, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String reference,
            String status,
            String failureCode,
            String failureMessage) {
    }
}
