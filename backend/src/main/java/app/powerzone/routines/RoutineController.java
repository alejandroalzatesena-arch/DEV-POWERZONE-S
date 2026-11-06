package app.powerzone.routines;

import app.powerzone.routines.dto.GenerateRoutineRequest;
import app.powerzone.routines.dto.RoutineDayResponse;
import app.powerzone.routines.dto.RoutineDetailResponse;
import app.powerzone.routines.dto.RoutineSummaryResponse;
import app.powerzone.routines.dto.RoutineWeekResponse;
import app.powerzone.routines.dto.UpdatePrescriptionRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    /** Genera una rutina para el solicitante o, si es personal, para un miembro. */
    @PostMapping("/generate")
    public ResponseEntity<RoutineDetailResponse> generate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) GenerateRoutineRequest request) {
        Long targetUserId = request == null ? null : request.userId();
        RoutineDetailResponse routine = routineService.generate(
                subjectId(jwt), gymId(jwt), staff(jwt), targetUserId);
        return ResponseEntity.created(URI.create("/api/v1/routines/" + routine.id()))
                .body(routine);
    }

    @GetMapping
    public List<RoutineSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) Long userId) {
        return routineService.list(subjectId(jwt), gymId(jwt), staff(jwt), userId);
    }

    @GetMapping("/{routineId}")
    public RoutineDetailResponse detail(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable long routineId) {
        return routineService.detail(routineId, subjectId(jwt), gymId(jwt), staff(jwt));
    }

    @GetMapping("/{routineId}/week")
    public RoutineWeekResponse week(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable long routineId) {
        return routineService.week(routineId, subjectId(jwt), gymId(jwt), staff(jwt));
    }

    @GetMapping("/{routineId}/days/{dayNumber}")
    public RoutineDayResponse day(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable long routineId,
                                  @PathVariable int dayNumber) {
        return routineService.day(
                routineId, dayNumber, subjectId(jwt), gymId(jwt), staff(jwt));
    }

    /** Ajuste de series, repeticiones y descanso por entrenador o administrador. */
    @PutMapping("/{routineId}/exercises/{routineExerciseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<Void> updatePrescription(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long routineId,
            @PathVariable long routineExerciseId,
            @Valid @RequestBody UpdatePrescriptionRequest request) {
        routineService.updatePrescription(
                routineId, gymId(jwt), routineExerciseId, request);
        return ResponseEntity.noContent().build();
    }

    /** Archiva la rutina conservando el historial de progreso asociado. */
    @DeleteMapping("/{routineId}")
    public ResponseEntity<Void> archive(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable long routineId) {
        routineService.archive(routineId, subjectId(jwt), gymId(jwt), staff(jwt));
        return ResponseEntity.noContent().build();
    }

    static long subjectId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    static long gymId(Jwt jwt) {
        Object claim = jwt.getClaim("gymId");
        return claim instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(claim));
    }

    static boolean staff(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("ADMIN") || roles.contains("TRAINER")
                || roles.contains("SUPER_ADMIN"));
    }
}
