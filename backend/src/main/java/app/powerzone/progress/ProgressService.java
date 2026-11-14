package app.powerzone.progress;

import app.powerzone.common.ApiException;
import app.powerzone.progress.dto.ProgressRequest;
import app.powerzone.progress.dto.ProgressResponse;
import app.powerzone.progress.dto.ProgressSummaryResponse;
import app.powerzone.routines.Exercise;
import app.powerzone.routines.ExerciseRepository;
import app.powerzone.routines.RoutineRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de cargas y resumen de evolucion por ejercicio. Cada entrada
 * pertenece siempre al usuario autenticado; no se acepta `userId` en el cuerpo.
 */
@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final ExerciseRepository exerciseRepository;
    private final RoutineRepository routineRepository;

    public ProgressService(ProgressRepository progressRepository,
                           ExerciseRepository exerciseRepository,
                           RoutineRepository routineRepository) {
        this.progressRepository = progressRepository;
        this.exerciseRepository = exerciseRepository;
        this.routineRepository = routineRepository;
    }

    @Transactional
    public ProgressResponse register(long userId, ProgressRequest request) {
        if (request.weightKg() == null && request.reps() == null && request.sets() == null) {
            throw ApiException.badRequest(
                    "EMPTY_PROGRESS",
                    "Registra al menos peso, repeticiones o series.");
        }

        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .filter(Exercise::active)
                .orElseThrow(() -> ApiException.notFound(
                        "EXERCISE_NOT_FOUND", "No se encontro el ejercicio solicitado."));

        if (request.routineExerciseId() != null
                && !routineRepository.belongsToUser(request.routineExerciseId(), userId)) {
            throw ApiException.forbidden(
                    "PROGRESS_FORBIDDEN",
                    "El ejercicio prescrito no pertenece a una rutina del usuario.");
        }

        LocalDate performedOn = request.performedOn() == null
                ? LocalDate.now()
                : request.performedOn();

        long id = progressRepository.insert(
                userId,
                exercise.id(),
                request.routineExerciseId(),
                performedOn,
                request.weightKg(),
                request.reps(),
                request.sets(),
                blankToNull(request.notes()));

        return progressRepository.findById(id)
                .map(ProgressResponse::from)
                .orElseThrow(() -> new IllegalStateException(
                        "El progreso registrado no se pudo recuperar"));
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> list(long userId,
                                       Long exerciseId,
                                       LocalDate from,
                                       LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest(
                    "INVALID_DATE_RANGE",
                    "La fecha inicial no puede ser posterior a la final.");
        }
        return progressRepository.findByUser(userId, exerciseId, from, to).stream()
                .map(ProgressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressSummaryResponse> summary(long userId) {
        return progressRepository.summaryByUser(userId).stream()
                .map(ProgressSummaryResponse::from)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
