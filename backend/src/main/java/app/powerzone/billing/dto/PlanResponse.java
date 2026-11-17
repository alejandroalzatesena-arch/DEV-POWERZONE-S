package app.powerzone.billing.dto;

import app.powerzone.billing.Plan;

public record PlanResponse(
        long id,
        String code,
        String name,
        String description,
        int priceCents,
        String currency,
        int durationDays) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.id(), plan.code(), plan.name(), plan.description(),
                plan.priceCents(), plan.currency(), plan.durationDays());
    }
}
