package app.powerzone.routines;

import app.powerzone.common.ApiException;
import app.powerzone.routines.dto.RoutineDayResponse;
import app.powerzone.routines.dto.RoutineDetailResponse;
import app.powerzone.routines.dto.RoutineSummaryResponse;
import app.powerzone.routines.dto.RoutineWeekResponse;
import app.powerzone.routines.dto.UpdatePrescriptionRequest;
import app.powerzone.routines.plan.PlannedDay;
import app.powerzone.routines.plan.PlannedExercise;
import app.powerzone.routines.plan.RoutinePlan;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta la generacion, consulta y mantenimiento de rutinas. Las reglas de
 * acceso se resuelven aqui: el propietario ve sus rutinas y el personal del
 * gimnasio (ADMIN / TRAINER) puede consultar y ajustar las de sus miembros.
 */
@Service
public class RoutineService {

    private final RoutineGeneratorService generatorService;
    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;

    public RoutineService(RoutineGeneratorService generatorService,
                          RoutineRepository routineRepository,
                          UserRepository userRepository) {
        this.generatorService = generatorService;
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RoutineDetailResponse generate(long requesterId,
                                          long requesterGymId,
                                          boolean staff,
                                          Long targetUserId) {
        long userId = targetUserId == null ? requesterId : targetUserId;
        if (userId != requesterId && !staff) {
            throw ApiException.forbidden(
                    "ROUTINE_FORBIDDEN",
                    "Solo un entrenador o administrador puede generar rutinas para otro miembro.");
        }

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario solicitado."));

        if (user.gymId() != requesterGymId) {
            throw ApiException.forbidden(
                    "ROUTINE_FORBIDDEN", "El miembro pertenece a otro gimnasio.");
        }

        RoutinePlan plan = generatorService.generate(user);
        long routineId = persist(user, plan);

        return routineRepository.findDetail(routineId)
                .map(RoutineDetailResponse::from)
                .orElseThrow(() -> new IllegalStateException(
                        "La rutina generada no se pudo recuperar"));
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> list(long requesterId,
                                             long requesterGymId,
                                             boolean staff,
                                             Long userId) {
        long targetUserId = userId == null ? requesterId : userId;
        if (targetUserId != requesterId) {
            if (!staff) {
                throw ApiException.forbidden(
                        "ROUTINE_FORBIDDEN",
                        "No puedes consultar las rutinas de otro miembro.");
            }
            UserAccount user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> ApiException.notFound(
                            "USER_NOT_FOUND", "No se encontro el usuario solicitado."));
            if (user.gymId() != requesterGymId) {
                throw ApiException.forbidden(
                        "ROUTINE_FORBIDDEN", "El miembro pertenece a otro gimnasio.");
            }
        }

        return routineRepository.findByUser(targetUserId).stream()
                .map(RoutineSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse detail(long routineId,
                                        long requesterId,
                                        long requesterGymId,
                                        boolean staff) {
        return RoutineDetailResponse.from(
                loadAuthorized(routineId, requesterId, requesterGymId, staff));
    }

    @Transactional(readOnly = true)
    public RoutineWeekResponse week(long routineId,
                                    long requesterId,
                                    long requesterGymId,
                                    boolean staff) {
        return RoutineWeekResponse.from(
                loadAuthorized(routineId, requesterId, requesterGymId, staff));
    }

    @Transactional(readOnly = true)
    public RoutineDayResponse day(long routineId,
                                  int dayNumber,
                                  long requesterId,
                                  long requesterGymId,
                                  boolean staff) {
        loadAuthorized(routineId, requesterId, requesterGymId, staff);
        RoutineDay day = routineRepository.findDay(routineId, dayNumber)
                .orElseThrow(() -> ApiException.notFound(
                        "ROUTINE_DAY_NOT_FOUND",
                        "No se encontro el dia solicitado en la rutina."));
        return RoutineDayResponse.from(day);
    }

    @Transactional
    public void updatePrescription(long routineId,
                                   long requesterGymId,
                                   long routineExerciseId,
                                   UpdatePrescriptionRequest request) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> ApiException.notFound(
                        "ROUTINE_NOT_FOUND", "No se encontro la rutina."));
        if (routine.gymId() != requesterGymId) {
            throw ApiException.forbidden(
                    "ROUTINE_FORBIDDEN", "La rutina pertenece a otro gimnasio.");
        }
        if (!routineRepository.belongsToRoutine(routineExerciseId, routineId)) {
            throw ApiException.notFound(
                    "ROUTINE_EXERCISE_NOT_FOUND",
                    "El ejercicio no pertenece a la rutina indicada.");
        }
        routineRepository.updatePrescription(
                routineExerciseId,
                request.sets(),
                request.reps(),
                request.restSeconds(),
                blankToNull(request.notes()));
    }

    @Transactional
    public void archive(long routineId,
                        long requesterId,
                        long requesterGymId,
                        boolean staff) {
        loadAuthorized(routineId, requesterId, requesterGymId, staff);
        routineRepository.updateStatus(routineId, "ARCHIVED");
    }

    private long persist(UserAccount user, RoutinePlan plan) {
        long routineId = routineRepository.insertRoutine(
                user.gymId(),
                user.id(),
                plan.name(),
                plan.goal(),
                plan.experienceLevel(),
                plan.daysPerWeek(),
                "GENERATED");

        for (PlannedDay day : plan.days()) {
            long dayId = routineRepository.insertDay(
                    routineId, day.dayNumber(), day.name(), day.focus());
            for (PlannedExercise exercise : day.exercises()) {
                routineRepository.insertExercise(
                        dayId,
                        exercise.exerciseId(),
                        exercise.position(),
                        exercise.sets(),
                        exercise.reps(),
                        exercise.restSeconds(),
                        exercise.notes());
            }
        }
        return routineId;
    }

    private RoutineDetail loadAuthorized(long routineId,
                                         long requesterId,
                                         long requesterGymId,
                                         boolean staff) {
        RoutineDetail detail = routineRepository.findDetail(routineId)
                .orElseThrow(() -> ApiException.notFound(
                        "ROUTINE_NOT_FOUND", "No se encontro la rutina solicitada."));

        Routine routine = detail.routine();
        if (routine.userId() == requesterId) {
            return detail;
        }
        if (!staff || routine.gymId() != requesterGymId) {
            throw ApiException.forbidden(
                    "ROUTINE_FORBIDDEN", "No tienes acceso a esta rutina.");
        }
        return detail;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
