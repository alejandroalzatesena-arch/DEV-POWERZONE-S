package app.powerzone.routines;

/** Nivel de experiencia declarado en el perfil del miembro. */
public enum ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    public static ExperienceLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nivel de experiencia es obligatorio");
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nivel de experiencia no soportado: " + value);
        }
    }
}
