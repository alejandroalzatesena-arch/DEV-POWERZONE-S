package app.powerzone.routines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.powerzone.common.ApiException;
import app.powerzone.routines.plan.PlannedExercise;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RoutineGeneratorServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ExerciseRepository exerciseRepository;

    private RoutineGeneratorService generator;

    @BeforeEach
    void setUp() {
        generator = new RoutineGeneratorService(userRepository, exerciseRepository);
    }

    @Test
    @DisplayName("genera un plan con un dia por dia disponible y ejercicios sin repetir en la sesion")
    void generatesBalancedPlan() {
        UserAccount member = member("STRENGTH", "INTERMEDIATE", 4);
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));
        when(exerciseRepository.findActiveByGroups(any())).thenReturn(catalog());

        var plan = generator.generateFor(5L);

        assertThat(plan.name()).isEqualTo("Fuerza - 4 dias");
        assertThat(plan.goal()).isEqualTo("STRENGTH");
        assertThat(plan.experienceLevel()).isEqualTo("INTERMEDIATE");
        assertThat(plan.days()).hasSize(4);

        for (var day : plan.days()) {
            assertThat(day.exercises()).hasSize(5);
            List<Long> ids = day.exercises().stream().map(PlannedExercise::exerciseId).toList();
            assertThat(ids).doesNotHaveDuplicates();
            assertThat(ids).hasSize(5);
            assertThat(day.exercises()).extracting(PlannedExercise::position)
                    .containsExactly(1, 2, 3, 4, 5);
        }

        List<PlannedExercise> firstDay = plan.days().get(0).exercises();
        assertThat(firstDay.get(0).sets()).isEqualTo(4);
        assertThat(firstDay.get(0).reps()).isEqualTo("4-6");
        assertThat(firstDay.get(0).restSeconds()).isEqualTo(150);
        assertThat(firstDay.get(0).notes()).isNotBlank();
        assertThat(firstDay.get(1).notes()).isNull();
    }

    @Test
    @DisplayName("rechaza la generacion cuando falta el objetivo del perfil")
    void rejectsIncompleteProfile() {
        UserAccount member = member(null, "INTERMEDIATE", 4);
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> generator.generateFor(5L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("PROFILE_INCOMPLETE");
                    assertThat(api.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    @DisplayName("rechaza dias por semana fuera del rango permitido")
    void rejectsInvalidDaysPerWeek() {
        UserAccount member = member("STRENGTH", "BEGINNER", 1);
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> generator.generateFor(5L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("INVALID_DAYS_PER_WEEK"));
    }

    @Test
    @DisplayName("rechaza la generacion cuando el catalogo no tiene ejercicios activos")
    void rejectsEmptyCatalog() {
        UserAccount member = member("HYPERTROPHY", "BEGINNER", 3);
        when(userRepository.findById(5L)).thenReturn(Optional.of(member));
        when(exerciseRepository.findActiveByGroups(any())).thenReturn(List.of());

        assertThatThrownBy(() -> generator.generateFor(5L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("EXERCISE_CATALOG_EMPTY"));
    }

    @Test
    @DisplayName("informa cuando el usuario solicitado no existe")
    void rejectsUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> generator.generateFor(99L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("USER_NOT_FOUND"));
    }

    /**
     * Catalogo de prueba con seis variantes por grupo muscular, suficiente para
     * los splits de dos a siete dias sin repetir ejercicios en una sesion.
     */
    private static List<Exercise> catalog() {
        Set<String> groups = Set.of(
                "CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "CORE", "FULL_BODY", "CARDIO");
        List<Exercise> exercises = new ArrayList<>();
        long id = 1;
        for (String group : groups) {
            for (int variant = 1; variant <= 6; variant++) {
                exercises.add(new Exercise(
                        id++,
                        group + " " + variant,
                        group,
                        variant % 2 == 0 ? "DUMBBELL" : "BARBELL",
                        "INTERMEDIATE",
                        "Ejercicio de prueba",
                        true));
            }
        }
        return exercises;
    }

    private static UserAccount member(String goal, String level, Integer daysPerWeek) {
        return new UserAccount(
                5L, 1L, "ana@powerzone.app", "hash", "Ana", "Gomez", null,
                "MEMBER", true, LocalDate.of(1998, 3, 14),
                new BigDecimal("168.5"), new BigDecimal("62.0"),
                goal, level, daysPerWeek);
    }
}
