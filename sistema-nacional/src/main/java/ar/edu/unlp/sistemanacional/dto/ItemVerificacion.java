package ar.edu.unlp.sistemanacional.dto;

// Chequeo de un aporte antes de comprometerlo. ok = false → motivo explica qué restricción no cumple.
public record ItemVerificacion(
        String ofertaRef,
        String cuit,
        String tipoRecurso,
        int cantidad,
        int disponible,
        boolean ok,
        String motivo
) {
}
