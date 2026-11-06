package app.powerzone.progress;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Agregado por ejercicio usado en la pantalla de progreso. */
public record ProgressSummary(
        long exerciseId,
        String exerciseName,
        int sessions,
        BigDecimal maxWeightKg,
        BigDecimal lastWeightKg,
        LocalDate lastPerformedOn) {
}
