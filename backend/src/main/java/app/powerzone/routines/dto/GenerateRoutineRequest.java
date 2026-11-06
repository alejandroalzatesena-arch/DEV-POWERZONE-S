package app.powerzone.routines.dto;

/**
 * Solicitud de generacion. Sin {@code userId} se genera para el solicitante;
 * con {@code userId}, un entrenador o administrador genera para un miembro de
 * su gimnasio.
 */
public record GenerateRoutineRequest(Long userId) {
}
