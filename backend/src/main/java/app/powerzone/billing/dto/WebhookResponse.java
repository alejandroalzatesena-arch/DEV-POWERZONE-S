package app.powerzone.billing.dto;

public record WebhookResponse(boolean received, boolean duplicate, String outcome) {
}
