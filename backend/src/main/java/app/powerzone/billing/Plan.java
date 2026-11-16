package app.powerzone.billing;

/** Plan de membresia vendido por un gimnasio. */
public record Plan(
        long id,
        long gymId,
        String code,
        String name,
        String description,
        int priceCents,
        String currency,
        int durationDays,
        boolean active) {
}
