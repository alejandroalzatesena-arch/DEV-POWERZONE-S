package app.powerzone.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.common.ApiException;
import app.powerzone.progress.dto.ProgressRequest;
import app.powerzone.routines.Exercise;
import app.powerzone.routines.ExerciseRepository;
import app.powerzone.routines.RoutineRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ProgressRepository progressRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private RoutineRepository routineRepository;

    private ProgressService progressService;

    @BeforeEach
    void setUp() {
        progressService = new ProgressService(
                progressRepository, exerciseRepository, routineRepository);
    }

    @Test
    @DisplayName("registra una entrada con la fecha actual cuando no se indica")
    void registersEntryWithToday() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise(true)));
        when(progressRepository.insert(
                eq(5L), eq(10L), isNull(), any(LocalDate.class),
                eq(new BigDecimal("80.5")), eq(8), eq(4), isNull()))
                .thenReturn(77L);
        when(progressRepository.findById(77L)).thenReturn(Optional.of(entry(77L, null)));

        var response = progressService.register(
                5L, new ProgressRequest(10L, null, null, new BigDecimal("80.5"), 8, 4, "  "));

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.exerciseName()).isEqualTo("Press banca");
        assertThat(response.weightKg()).isEqualByComparingTo("82.0");
    }

    @Test
    @DisplayName("rechaza registros sin ninguna metrica")
    void rejectsEmptyProgress() {
        assertThatThrownBy(() -> progressService.register(
                5L, new ProgressRequest(10L, null, null, null, null, null, "nota")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("EMPTY_PROGRESS");
                    assertThat(api.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verifyNoInteractions(progressRepository, exerciseRepository);
    }

    @Test
    @DisplayName("rechaza ejercicios inexistentes o inactivos")
    void rejectsUnknownExercise() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise(false)));

        assertThatThrownBy(() -> progressService.register(
                5L, new ProgressRequest(10L, null, null, null, null, 3, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("EXERCISE_NOT_FOUND"));

        verify(progressRepository, never()).insert(
                anyLong(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("rechaza asociar una rutina que no pertenece al usuario")
    void rejectsForeignRoutineExercise() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise(true)));
        when(routineRepository.belongsToUser(300L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> progressService.register(
                5L, new ProgressRequest(10L, 300L, null, null, 8, 4, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PROGRESS_FORBIDDEN");
                    assertThat(api.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("acepta la rutina cuando el ejercicio prescrito pertenece al usuario")
    void acceptsOwnRoutineExercise() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise(true)));
        when(routineRepository.belongsToUser(300L, 5L)).thenReturn(true);
        when(progressRepository.insert(
                eq(5L), eq(10L), eq(300L), eq(LocalDate.of(2026, 11, 9)),
                eq(new BigDecimal("82.0")), eq(6), eq(4), eq("Buena tecnica")))
                .thenReturn(77L);
        when(progressRepository.findById(77L)).thenReturn(Optional.of(entry(77L, 300L)));

        var response = progressService.register(5L, new ProgressRequest(
                10L, 300L, LocalDate.of(2026, 11, 9),
                new BigDecimal("82.0"), 6, 4, "Buena tecnica"));

        assertThat(response.routineExerciseId()).isEqualTo(300L);
        assertThat(response.notes()).isEqualTo("Buena tecnica");
    }

    @Test
    @DisplayName("rechaza rangos de fecha invertidos al consultar el historial")
    void rejectsInvalidDateRange() {
        assertThatThrownBy(() -> progressService.list(
                5L, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_DATE_RANGE"));

        verifyNoInteractions(progressRepository);
    }

    @Test
    @DisplayName("devuelve el historial filtrado del usuario")
    void listsHistory() {
        when(progressRepository.findByUser(
                5L, 10L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(entry(77L, null), entry(78L, null)));

        var entries = progressService.list(
                5L, 10L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(entry -> entry.exerciseId()).containsOnly(10L);
    }

    @Test
    @DisplayName("devuelve el resumen agregado por ejercicio")
    void summarizesProgress() {
        when(progressRepository.summaryByUser(5L)).thenReturn(List.of(
                new ProgressSummary(10L, "Press banca", 4,
                        new BigDecimal("90.0"), new BigDecimal("85.0"),
                        LocalDate.of(2026, 11, 9))));

        var summary = progressService.summary(5L);

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).sessions()).isEqualTo(4);
        assertThat(summary.get(0).maxWeightKg()).isEqualByComparingTo("90.0");
        assertThat(summary.get(0).lastPerformedOn()).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    private static Exercise exercise(boolean active) {
        return new Exercise(
                10L, "Press banca", "CHEST", "BARBELL", "INTERMEDIATE",
                "Empuje horizontal", active);
    }

    private static ProgressEntry entry(long id, Long routineExerciseId) {
        return new ProgressEntry(
                id, 5L, 10L, "Press banca", routineExerciseId, LocalDate.of(2026, 11, 9),
                new BigDecimal("82.0"), 6, 4, "Buena tecnica",
                Instant.parse("2026-11-09T15:30:00Z"));
    }
}
