package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.TipoRecurso;

public record TipoRecursoDTO(
        String codigo,
        String nombre,
        String categoria,
        String unidad,
        String certificacionRequerida   // null = no requiere
) {
    public static TipoRecursoDTO desde(TipoRecurso t) {
        return new TipoRecursoDTO(
                t.getCodigo(),
                t.getNombre(),
                t.getCategoria().name(),
                t.getUnidad(),
                t.getCertificacionRequerida() == null ? null : t.getCertificacionRequerida().getCodigo()
        );
    }
}
