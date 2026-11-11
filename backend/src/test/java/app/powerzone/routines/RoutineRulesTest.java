package app.powerzone.routines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoutineRulesTest {

    @Test
    @DisplayName("aplica la prescripcion base de fuerza y reduce series al principiante")
    void prescribesStrengthForBeginner() {
        var prescription = RoutineRules.prescriptionFor(Goal.STRENGTH, ExperienceLevel.BEGINNER);

        assertThat(prescription.sets()).isEqualTo(3);
        assertThat(prescription.reps()).isEqualTo("4-6");
        assertThat(prescription.restSeconds()).isEqualTo(165);
    }

    @Test
    @DisplayName("aumenta una serie al avanzado sin cambiar repeticiones")
    void prescribesHypertrophyForAdvanced() {
        var prescription = RoutineRules.prescriptionFor(Goal.HYPERTROPHY, ExperienceLevel.ADVANCED);

        assertThat(prescription.sets()).isEqualTo(5);
        assertThat(prescription.reps()).isEqualTo("8-12");
        assertThat(prescription.restSeconds()).isEqualTo(90);
    }

    @Test
    @DisplayName("mantiene la prescripcion base en el nivel intermedio")
    void keepsBasePrescriptionForIntermediate() {
        var prescription = RoutineRules.prescriptionFor(Goal.WEIGHT_LOSS, ExperienceLevel.INTERMEDIATE);

        assertThat(prescription.sets()).isEqualTo(3);
        assertThat(prescription.reps()).isEqualTo("12-15");
        assertThat(prescription.restSeconds()).isEqualTo(45);
    }

    @Test
    @DisplayName("reduce series y amplia descanso en resistencia para principiantes")
    void prescribesEnduranceForBeginner() {
        var prescription = RoutineRules.prescriptionFor(Goal.ENDURANCE, ExperienceLevel.BEGINNER);

        assertThat(prescription.sets()).isEqualTo(2);
        assertThat(prescription.reps()).isEqualTo("15-20");
        assertThat(prescription.restSeconds()).isEqualTo(45);
    }

    @Test
    @DisplayName("asigna de cuatro a seis ejercicios por dia segun la experiencia")
    void choosesExercisesPerDay() {
        assertThat(RoutineRules.exercisesPerDay(ExperienceLevel.BEGINNER)).isEqualTo(4);
        assertThat(RoutineRules.exercisesPerDay(ExperienceLevel.INTERMEDIATE)).isEqualTo(5);
        assertThat(RoutineRules.exercisesPerDay(ExperienceLevel.ADVANCED)).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7})
    @DisplayName("genera un split con un dia por cada dia disponible")
    void buildsSplitForAllowedDays(int daysPerWeek) {
        var split = RoutineRules.splitFor(daysPerWeek);

        assertThat(split).hasSize(daysPerWeek);
        assertThat(split).allSatisfy(day -> {
            assertThat(day.name()).isNotBlank();
            assertThat(day.focus()).isNotBlank();
            assertThat(day.muscleGroups()).isNotEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 8, 10})
    @DisplayName("rechaza cantidades de dias fuera del rango permitido")
    void rejectsDaysOutOfRange(int daysPerWeek) {
        assertThatThrownBy(() -> RoutineRules.splitFor(daysPerWeek))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compone el nombre de la rutina con objetivo y dias")
    void buildsRoutineName() {
        assertThat(RoutineRules.routineName(Goal.HYPERTROPHY, 4))
                .isEqualTo("Hipertrofia - 4 dias");
        assertThat(RoutineRules.routineName(Goal.WEIGHT_LOSS, 3))
                .isEqualTo("Perdida de peso - 3 dias");
    }
}
