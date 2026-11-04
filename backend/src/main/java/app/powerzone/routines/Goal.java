package app.powerzone.routines;

/** Objetivo de entrenamiento soportado por el motor de rutinas. */
public enum Goal {
    STRENGTH,
    HYPERTROPHY,
    WEIGHT_LOSS,
    ENDURANCE;

    public static Goal from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El objetivo es obligatorio");
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Objetivo no soportado: " + value);
        }
    }
}
