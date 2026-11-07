package app.powerzone.progress.dto;

import app.powerzone.progress.ProgressSummary;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressSummaryResponse(
        long exerciseId,
        String exerciseName,
        int sessions,
        BigDecimal maxWeightKg,
        BigDecimal lastWeightKg,
        LocalDate lastPerformedOn) {

    public static ProgressSummaryResponse from(ProgressSummary summary) {
        return new ProgressSummaryResponse(
                summary.exerciseId(),
                summary.exerciseName(),
                summary.sessions(),
                summary.maxWeightKg(),
                summary.lastWeightKg(),
                summary.lastPerformedOn());
    }
}
