package ar.edu.unlp.sistemanacional.dto;

import java.util.List;

// Disponibilidad global de un tipo de recurso (solo cuentan las ONGs HABILITADAS)
public record DisponibilidadDTO(
        String tipoRecurso,
        String nombre,
        String unidad,
        int total,
        int comprometido,
        int disponible,
        List<DisponibilidadOngDTO> porOng
) {
    public record DisponibilidadOngDTO(String cuit, String nombre, int total, int comprometido, int disponible) {
    }
}
