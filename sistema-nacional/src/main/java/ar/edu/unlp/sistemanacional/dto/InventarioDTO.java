package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.RecursoInventario;

public record InventarioDTO(
        String tipoRecurso,
        String nombre,
        String categoria,
        String unidad,
        int total,
        int comprometido,
        int disponible
) {
    public static InventarioDTO desde(RecursoInventario r) {
        return new InventarioDTO(
                r.getTipoRecurso().getCodigo(),
                r.getTipoRecurso().getNombre(),
                r.getTipoRecurso().getCategoria().name(),
                r.getTipoRecurso().getUnidad(),
                r.getCantidadTotal(),
                r.getCantidadComprometida(),
                r.getDisponible()
        );
    }
}
