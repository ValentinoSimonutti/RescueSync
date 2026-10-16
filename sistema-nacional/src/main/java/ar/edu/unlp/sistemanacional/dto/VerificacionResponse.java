package ar.edu.unlp.sistemanacional.dto;

import java.util.List;

// factible = true si POST /api/compromisos con el mismo body se podría ejecutar ahora
public record VerificacionResponse(String emergenciaRef, boolean factible, List<ItemVerificacion> items) {
}
