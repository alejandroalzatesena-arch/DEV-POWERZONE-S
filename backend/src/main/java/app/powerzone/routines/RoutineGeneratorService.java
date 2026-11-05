package app.powerzone.routines;

import app.powerzone.common.ApiException;
import app.powerzone.routines.plan.PlannedDay;
import app.powerzone.routines.plan.PlannedExercise;
import app.powerzone.routines.plan.RoutinePlan;
import app.powerzone.users.UserAccount;
import app.powerzone.users.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de generacion de rutinas. Combina el perfil del miembro con las reglas
 * de {@link RoutineRules} y el catalogo de ejercicios para producir un plan
 * semanal equilibrado y sin repetir ejercicios dentro del mismo dia.
 */
@Service
public class RoutineGeneratorService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    public RoutineGeneratorService(UserRepository userRepository,
                                   ExerciseRepository exerciseRepository) {
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public RoutinePlan generateFor(long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "USER_NOT_FOUND", "No se encontro el usuario solicitado."));
        return generate(user);
    }

    @Transactional(readOnly = true)
    public RoutinePlan generate(UserAccount user) {
        validateProfile(user);

        Goal goal = Goal.from(user.goal());
        ExperienceLevel level = ExperienceLevel.from(user.experienceLevel());
        int daysPerWeek = user.daysPerWeek();

        List<RoutineRules.DayTemplate> split = RoutineRules.splitFor(daysPerWeek);
        Map<String, List<Exercise>> catalog = loadCatalog(split);
        RoutineRules.Prescription prescription = RoutineRules.prescriptionFor(goal, level);
        int exercisesPerDay = RoutineRules.exercisesPerDay(level);

        Map<Long, Integer> usage = new HashMap<>();
        List<PlannedDay> days = new ArrayList<>();
        for (int index = 0; index < split.size(); index++) {
            RoutineRules.DayTemplate template = split.get(index);
            List<PlannedExercise> exercises = selectExercises(
                    template, exercisesPerDay, prescription, goal, catalog, usage);
            days.add(new PlannedDay(index + 1, template.name(), template.focus(), exercises));
        }

        return new RoutinePlan(
                RoutineRules.routineName(goal, daysPerWeek),
                goal.name(),
                level.name(),
                daysPerWeek,
                days);
    }

    private void validateProfile(UserAccount user) {
        List<String> missing = new ArrayList<>();
        if (user.goal() == null || user.goal().isBlank()) {
            missing.add("objetivo");
        }
        if (user.experienceLevel() == null || user.experienceLevel().isBlank()) {
            missing.add("nivel de experiencia");
        }
        if (user.daysPerWeek() == null) {
            missing.add("dias por semana");
        }

        if (!missing.isEmpty()) {
            throw ApiException.unprocessable(
                    "PROFILE_INCOMPLETE",
                    "Completa tu " + String.join(", ", missing)
                            + " antes de generar una rutina.");
        }

        int days = user.daysPerWeek();
        if (days < RoutineRules.MIN_DAYS || days > RoutineRules.MAX_DAYS) {
            throw ApiException.badRequest(
                    "INVALID_DAYS_PER_WEEK",
                    "Los dias por semana deben estar entre "
                            + RoutineRules.MIN_DAYS + " y " + RoutineRules.MAX_DAYS + ".");
        }
    }

    private Map<String, List<Exercise>> loadCatalog(List<RoutineRules.DayTemplate> split) {
        Set<String> groups = new HashSet<>();
        split.forEach(day -> groups.addAll(day.muscleGroups()));

        List<Exercise> exercises = exerciseRepository.findActiveByGroups(groups);
        if (exercises.isEmpty()) {
            throw ApiException.unprocessable(
                    "EXERCISE_CATALOG_EMPTY",
                    "El catalogo de ejercicios no tiene registros activos.");
        }

        Map<String, List<Exercise>> byGroup = new HashMap<>();
        for (Exercise exercise : exercises) {
            byGroup.computeIfAbsent(exercise.muscleGroup(), key -> new ArrayList<>())
                    .add(exercise);
        }
        return byGroup;
    }

    /**
     * Recorre los grupos musculares del dia en orden y elige, para cada uno, el
     * ejercicio menos usado en la semana. Asi el volumen se reparte entre
     * variantes y no se repite un ejercicio en la misma sesion.
     */
    private List<PlannedExercise> selectExercises(RoutineRules.DayTemplate template,
                                                  int target,
                                                  RoutineRules.Prescription prescription,
                                                  Goal goal,
                                                  Map<String, List<Exercise>> catalog,
                                                  Map<Long, Integer> usage) {
        List<PlannedExercise> selected = new ArrayList<>();
        Set<Long> usedToday = new HashSet<>();
        int guard = 0;

        while (selected.size() < target && guard++ < 100) {
            boolean added = false;
            for (String group : template.muscleGroups()) {
                if (selected.size() >= target) {
                    break;
                }
                Exercise candidate = nextCandidate(catalog.getOrDefault(group, List.of()),
                        usedToday, usage);
                if (candidate == null) {
                    continue;
                }

                usedToday.add(candidate.id());
                usage.merge(candidate.id(), 1, Integer::sum);
                selected.add(new PlannedExercise(
                        candidate.id(),
                        candidate.name(),
                        candidate.muscleGroup(),
                        selected.size() + 1,
                        prescription.sets(),
                        prescription.reps(),
                        prescription.restSeconds(),
                        selected.isEmpty() ? guidance(goal) : null));
                added = true;
            }

            if (!added) {
                break;
            }
        }

        if (selected.isEmpty()) {
            throw ApiException.unprocessable(
                    "EXERCISE_CATALOG_EMPTY",
                    "No hay ejercicios disponibles para el split solicitado.");
        }

        return List.copyOf(selected);
    }

    private Exercise nextCandidate(List<Exercise> candidates,
                                   Set<Long> usedToday,
                                   Map<Long, Integer> usage) {
        Exercise best = null;
        int bestUsage = Integer.MAX_VALUE;
        for (Exercise candidate : candidates) {
            if (usedToday.contains(candidate.id())) {
                continue;
            }
            int used = usage.getOrDefault(candidate.id(), 0);
            if (used < bestUsage) {
                best = candidate;
                bestUsage = used;
            }
        }
        return best;
    }

    private String guidance(Goal goal) {
        return switch (goal) {
            case STRENGTH -> "Series de aproximacion antes de la serie efectiva.";
            case HYPERTROPHY -> "Controla la fase de bajada en dos segundos.";
            case WEIGHT_LOSS -> "Manten ritmo constante y descansa el tiempo indicado.";
            case ENDURANCE -> "Ritmo sostenido sin perder la tecnica.";
        };
    }
}
