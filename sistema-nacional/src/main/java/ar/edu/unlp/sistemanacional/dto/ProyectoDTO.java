package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.Proyecto;

import java.time.LocalDateTime;
import java.util.List;

public record ProyectoDTO(
        String emergenciaRef,
        String gravedad,
        String descripcion,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre,
        List<CompromisoDTO> compromisos
) {
    public static ProyectoDTO desde(Proyecto p) {
        return new ProyectoDTO(
                p.getEmergenciaRef(),
                p.getGravedad() == null ? null : p.getGravedad().name(),
                p.getDescripcion(),
                p.getEstado().name(),
                p.getFechaInicio(),
                p.getFechaCierre(),
                p.getCompromisos().stream().map(CompromisoDTO::desde).toList()
        );
    }
}
