package app.powerzone.routines;

import java.util.List;

/**
 * Reglas de negocio del motor de rutinas: prescripcion por objetivo, split
 * segun los dias disponibles y ajuste por experiencia. Es una clase pura, sin
 * acceso a datos, para poder probarla de forma aislada.
 */
public final class RoutineRules {

    static final int MIN_DAYS = 2;
    static final int MAX_DAYS = 7;

    private RoutineRules() {
    }

    /** Series, repeticiones y descanso base antes del ajuste por experiencia. */
    public record Prescription(int sets, String reps, int restSeconds) {
    }

    /**
     * Plantilla de un dia. {@code muscleGroups} define el orden en que el
     * generador busca ejercicios.
     */
    public record DayTemplate(String name, String focus, List<String> muscleGroups) {
    }

    public static Prescription prescriptionFor(Goal goal, ExperienceLevel level) {
        Prescription base = switch (goal) {
            case STRENGTH -> new Prescription(4, "4-6", 150);
            case HYPERTROPHY -> new Prescription(4, "8-12", 90);
            case WEIGHT_LOSS -> new Prescription(3, "12-15", 45);
            case ENDURANCE -> new Prescription(3, "15-20", 30);
        };

        return switch (level) {
            case BEGINNER -> new Prescription(
                    Math.max(2, base.sets() - 1), base.reps(), base.restSeconds() + 15);
            case INTERMEDIATE -> base;
            case ADVANCED -> new Prescription(
                    Math.min(6, base.sets() + 1), base.reps(), base.restSeconds());
        };
    }

    /** Cantidad de ejercicios por sesion segun la experiencia del miembro. */
    public static int exercisesPerDay(ExperienceLevel level) {
        return switch (level) {
            case BEGINNER -> 4;
            case INTERMEDIATE -> 5;
            case ADVANCED -> 6;
        };
    }

    /**
     * Split semanal. Hasta tres dias se trabaja el cuerpo completo; con cuatro
     * se alterna tren superior e inferior y con cinco o mas se usa el split
     * empuje / traccion / pierna.
     */
    public static List<DayTemplate> splitFor(int daysPerWeek) {
        if (daysPerWeek < MIN_DAYS || daysPerWeek > MAX_DAYS) {
            throw new IllegalArgumentException(
                    "Los dias por semana deben estar entre " + MIN_DAYS + " y " + MAX_DAYS);
        }

        return switch (daysPerWeek) {
            case 2 -> List.of(
                    new DayTemplate("Cuerpo completo A", "Fuerza global",
                            List.of("LEGS", "CHEST", "BACK", "CORE")),
                    new DayTemplate("Cuerpo completo B", "Fuerza global",
                            List.of("LEGS", "SHOULDERS", "BACK", "ARMS")));
            case 3 -> List.of(
                    new DayTemplate("Cuerpo completo A", "Empuje y pierna",
                            List.of("LEGS", "CHEST", "SHOULDERS", "CORE")),
                    new DayTemplate("Cuerpo completo B", "Traccion y pierna",
                            List.of("LEGS", "BACK", "ARMS", "CORE")),
                    new DayTemplate("Cuerpo completo C", "Global y core",
                            List.of("LEGS", "CHEST", "BACK", "FULL_BODY")));
            case 4 -> List.of(
                    new DayTemplate("Tren superior A", "Empuje",
                            List.of("CHEST", "SHOULDERS", "ARMS", "BACK")),
                    new DayTemplate("Tren inferior A", "Pierna y core",
                            List.of("LEGS", "CORE", "FULL_BODY")),
                    new DayTemplate("Tren superior B", "Traccion",
                            List.of("BACK", "ARMS", "SHOULDERS", "CHEST")),
                    new DayTemplate("Tren inferior B", "Pierna y gluteo",
                            List.of("LEGS", "CORE", "CARDIO")));
            case 5 -> List.of(
                    new DayTemplate("Empuje", "Pecho, hombro y triceps",
                            List.of("CHEST", "SHOULDERS", "ARMS")),
                    new DayTemplate("Traccion", "Espalda y biceps",
                            List.of("BACK", "ARMS", "CORE")),
                    new DayTemplate("Pierna", "Cuadriceps, femoral y gemelo",
                            List.of("LEGS", "CORE")),
                    new DayTemplate("Tren superior", "Volumen de tren superior",
                            List.of("CHEST", "BACK", "SHOULDERS", "ARMS")),
                    new DayTemplate("Tren inferior", "Fuerza de pierna",
                            List.of("LEGS", "CORE", "FULL_BODY")));
            case 6 -> List.of(
                    new DayTemplate("Empuje A", "Pecho y hombro",
                            List.of("CHEST", "SHOULDERS", "ARMS")),
                    new DayTemplate("Traccion A", "Espalda y biceps",
                            List.of("BACK", "ARMS", "CORE")),
                    new DayTemplate("Pierna A", "Cuadriceps y gemelo",
                            List.of("LEGS", "CORE")),
                    new DayTemplate("Empuje B", "Hombro y triceps",
                            List.of("SHOULDERS", "CHEST", "ARMS")),
                    new DayTemplate("Traccion B", "Espalda y core",
                            List.of("BACK", "CORE", "ARMS")),
                    new DayTemplate("Pierna B", "Femoral y gluteo",
                            List.of("LEGS", "FULL_BODY", "CORE")));
            default -> List.of(
                    new DayTemplate("Empuje", "Pecho y hombro",
                            List.of("CHEST", "SHOULDERS", "ARMS")),
                    new DayTemplate("Traccion", "Espalda y biceps",
                            List.of("BACK", "ARMS", "CORE")),
                    new DayTemplate("Pierna", "Cuadriceps y femoral",
                            List.of("LEGS", "CORE")),
                    new DayTemplate("Tren superior", "Volumen superior",
                            List.of("CHEST", "BACK", "SHOULDERS", "ARMS")),
                    new DayTemplate("Tren inferior", "Fuerza inferior",
                            List.of("LEGS", "FULL_BODY", "CORE")),
                    new DayTemplate("Global", "Acondicionamiento",
                            List.of("FULL_BODY", "CARDIO", "CORE")),
                    new DayTemplate("Recuperacion activa", "Cardio suave y movilidad",
                            List.of("CARDIO", "CORE")));
        };
    }

    /** Nombre descriptivo de la rutina generada. */
    public static String routineName(Goal goal, int daysPerWeek) {
        String objetivo = switch (goal) {
            case STRENGTH -> "Fuerza";
            case HYPERTROPHY -> "Hipertrofia";
            case WEIGHT_LOSS -> "Perdida de peso";
            case ENDURANCE -> "Resistencia";
        };
        return objetivo + " - " + daysPerWeek + " dias";
    }
}
