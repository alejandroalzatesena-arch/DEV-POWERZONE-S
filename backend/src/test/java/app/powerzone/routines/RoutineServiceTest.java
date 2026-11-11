package app.powerzone.routines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.powerzone.common.ApiException;
import app.powerzone.routines.dto.UpdatePrescriptionRequest;
import app.powerzone.routines.plan.PlannedDay;
import app.powerzone.routines.plan.PlannedExercise;
import app.powerzone.routines.plan.RoutinePlan;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
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
class RoutineServiceTest {

    @Mock
    private RoutineGeneratorService generatorService;
    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private UserRepository userRepository;

    private RoutineService routineService;
    private UserAccount member;

    @BeforeEach
    void setUp() {
        routineService = new RoutineService(generatorService, routineRepository, userRepository);
        member = new UserAccount(
                5L, 1L, "ana@powerzone.app", "hash", "Ana", "Gomez", null,
                "MEMBER", true, LocalDate.of(1998, 3, 14),
                new BigDecimal("168.5"), new BigDecimal("62.0"),
                "STRENGTH", "INTERMEDIATE", 2);
    }

    @Test
    @DisplayName("genera y persiste la rutina del solicitante con dias y ejercicios")
    void generatesAndPersistsRoutine() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));
        when(generatorService.generate(member)).thenReturn(plan());
        when(routineRepository.insertRoutine(
                eq(1L), eq(5L), eq("Fuerza - 2 dias"), eq("STRENGTH"),
                eq("INTERMEDIATE"), eq(2), eq("GENERATED"))).thenReturn(100L);
        when(routineRepository.insertDay(100L, 1, "Cuerpo completo A", "Fuerza global"))
                .thenReturn(200L);
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));

        var response = routineService.generate(5L, 1L, false, null);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).exercises()).hasSize(1);
        verify(routineRepository).insertExercise(
                200L, 10L, 1, 4, "4-6", 150, "Serie de aproximacion");
    }

    @Test
    @DisplayName("impide que un miembro genere una rutina para otro usuario")
    void rejectsGenerationForOtherMemberWithoutStaffRole() {
        assertThatThrownBy(() -> routineService.generate(5L, 1L, false, 8L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("ROUTINE_FORBIDDEN");
                    assertThat(api.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verifyNoInteractions(generatorService, routineRepository);
    }

    @Test
    @DisplayName("impide que el personal genere rutinas para miembros de otro gimnasio")
    void rejectsGenerationForOtherGym() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(outsider()));

        assertThatThrownBy(() -> routineService.generate(5L, 1L, true, 8L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_FORBIDDEN"));

        verifyNoInteractions(generatorService);
    }

    @Test
    @DisplayName("el propietario consulta el detalle de su rutina")
    void returnsDetailForOwner() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));

        var response = routineService.detail(100L, 5L, 1L, false);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("Fuerza - 2 dias");
    }

    @Test
    @DisplayName("el personal del mismo gimnasio consulta rutinas de sus miembros")
    void allowsStaffOfSameGym() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 8L, 1L)));

        var response = routineService.week(100L, 5L, 1L, true);

        assertThat(response.routineId()).isEqualTo(100L);
        assertThat(response.days()).hasSize(1);
    }

    @Test
    @DisplayName("bloquea el detalle de rutinas ajenas sin rol de personal")
    void rejectsDetailForStranger() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 8L, 1L)));

        assertThatThrownBy(() -> routineService.detail(100L, 5L, 1L, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_FORBIDDEN"));
    }

    @Test
    @DisplayName("responde 404 cuando la rutina no existe")
    void reportsMissingRoutine() {
        when(routineRepository.findDetail(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.detail(404L, 5L, 1L, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_NOT_FOUND"));
    }

    @Test
    @DisplayName("consulta el dia solicitado de una rutina autorizada")
    void returnsDayOfAuthorizedRoutine() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));
        when(routineRepository.findDay(100L, 2)).thenReturn(Optional.of(day()));

        var response = routineService.day(100L, 2, 5L, 1L, false);

        assertThat(response.dayNumber()).isEqualTo(2);
        assertThat(response.exercises()).hasSize(1);
        assertThat(response.exercises().get(0).name()).isEqualTo("Press banca");
    }

    @Test
    @DisplayName("reporta 404 cuando el dia no pertenece a la rutina")
    void reportsMissingDay() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));
        when(routineRepository.findDay(100L, 9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.day(100L, 9, 5L, 1L, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_DAY_NOT_FOUND"));
    }

    @Test
    @DisplayName("el entrenador ajusta la prescripcion y las notas se normalizan")
    void updatesPrescription() {
        when(routineRepository.findById(100L)).thenReturn(Optional.of(routine(100L, 5L, 1L)));
        when(routineRepository.belongsToRoutine(300L, 100L)).thenReturn(true);

        routineService.updatePrescription(
                100L, 1L, 300L, new UpdatePrescriptionRequest(5, "6-8", 120, "  Subir carga  "));

        verify(routineRepository).updatePrescription(300L, 5, "6-8", 120, "Subir carga");
    }

    @Test
    @DisplayName("rechaza ajustar un ejercicio que no pertenece a la rutina")
    void rejectsPrescriptionOfForeignExercise() {
        when(routineRepository.findById(100L)).thenReturn(Optional.of(routine(100L, 5L, 1L)));
        when(routineRepository.belongsToRoutine(300L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> routineService.updatePrescription(
                100L, 1L, 300L, new UpdatePrescriptionRequest(4, "8-12", 90, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_EXERCISE_NOT_FOUND"));

        verify(routineRepository, never()).updatePrescription(
                anyLong(), anyInt(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("rechaza ajustar rutinas de otro gimnasio")
    void rejectsPrescriptionOfOtherGym() {
        when(routineRepository.findById(100L)).thenReturn(Optional.of(routine(100L, 5L, 2L)));

        assertThatThrownBy(() -> routineService.updatePrescription(
                100L, 1L, 300L, new UpdatePrescriptionRequest(4, "8-12", 90, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_FORBIDDEN"));

        verify(routineRepository, never()).updatePrescription(
                anyLong(), anyInt(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("archiva la rutina en lugar de borrarla para conservar el progreso")
    void archivesRoutine() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));

        routineService.archive(100L, 5L, 1L, false);

        verify(routineRepository).updateStatus(100L, "ARCHIVED");
    }

    @Test
    @DisplayName("lista las rutinas del solicitante sin consultar el usuario")
    void listsOwnRoutines() {
        when(routineRepository.findByUser(5L)).thenReturn(List.of(routine(100L, 5L, 1L)));

        var routines = routineService.list(5L, 1L, false, null);

        assertThat(routines).hasSize(1);
        assertThat(routines.get(0).id()).isEqualTo(100L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("impide listar las rutinas de otro miembro sin rol de personal")
    void rejectsListingForOtherMember() {
        assertThatThrownBy(() -> routineService.list(5L, 1L, false, 8L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("ROUTINE_FORBIDDEN"));

        verifyNoInteractions(routineRepository);
    }

    @Test
    @DisplayName("el personal lista las rutinas de un miembro de su gimnasio")
    void listsRoutinesForStaff() {
        when(userRepository.findById(8L)).thenReturn(Optional.of(outsiderSameGym()));
        when(routineRepository.findByUser(8L)).thenReturn(List.of(routine(100L, 8L, 1L)));

        var routines = routineService.list(5L, 1L, true, 8L);

        assertThat(routines).hasSize(1);
    }

    @Test
    @DisplayName("crea el detalle de respuesta con la jerarquia persistida")
    void buildsDetailResponse() {
        when(routineRepository.findDetail(100L)).thenReturn(Optional.of(detail(100L, 5L, 1L)));

        var response = routineService.detail(100L, 5L, 1L, false);

        assertThat(response.goal()).isEqualTo("STRENGTH");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.days().get(0).exercises().get(0).name()).isEqualTo("Press banca");
        assertThat(response.days().get(0).exercises().get(0).equipment()).isEqualTo("BARBELL");
    }

    private static RoutinePlan plan() {
        PlannedExercise exercise = new PlannedExercise(
                10L, "Press banca", "CHEST", 1, 4, "4-6", 150,
                "Serie de aproximacion");
        return new RoutinePlan(
                "Fuerza - 2 dias", "STRENGTH", "INTERMEDIATE", 2,
                List.of(new PlannedDay(1, "Cuerpo completo A", "Fuerza global", List.of(exercise))));
    }

    private static RoutineDetail detail(long routineId, long userId, long gymId) {
        return new RoutineDetail(routine(routineId, userId, gymId), List.of(day()));
    }

    private static Routine routine(long routineId, long userId, long gymId) {
        return new Routine(
                routineId, gymId, userId, "Fuerza - 2 dias", "STRENGTH", "INTERMEDIATE",
                2, "ACTIVE", "GENERATED",
                Instant.parse("2026-11-09T15:00:00Z"), Instant.parse("2026-11-09T15:00:00Z"));
    }

    private static RoutineDay day() {
        RoutineExercise exercise = new RoutineExercise(
                300L, 10L, "Press banca", "CHEST", "BARBELL", 1, 4, "4-6", 150, null);
        return new RoutineDay(200L, 2, "Tren superior A", "Empuje", List.of(exercise));
    }

    private static UserAccount outsider() {
        return new UserAccount(
                8L, 2L, "luis@powerzone.app", "hash", "Luis", "Rios", null,
                "MEMBER", true, null, null, null, "STRENGTH", "BEGINNER", 3);
    }

    private static UserAccount outsiderSameGym() {
        return new UserAccount(
                8L, 1L, "luis@powerzone.app", "hash", "Luis", "Rios", null,
                "MEMBER", true, null, null, null, "STRENGTH", "BEGINNER", 3);
    }
}
