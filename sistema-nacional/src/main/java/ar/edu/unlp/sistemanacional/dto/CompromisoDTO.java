package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.Compromiso;

import java.time.LocalDateTime;

public record CompromisoDTO(
        Long id,
        String emergenciaRef,
        String ofertaRef,
        String cuit,
        String ongNombre,
        String tipoRecurso,
        int cantidad,
        String rolAsignado,
        String estado,
        LocalDateTime fechaCompromiso,
        LocalDateTime fechaLiberacion
) {
    public static CompromisoDTO desde(Compromiso c) {
        return new CompromisoDTO(
                c.getId(),
                c.getProyecto().getEmergenciaRef(),
                c.getOfertaRef(),
                c.getOng().getCuit(),
                c.getOng().getNombre(),
                c.getTipoRecurso().getCodigo(),
                c.getCantidad(),
                c.getRolAsignado().name(),
                c.getEstado().name(),
                c.getFechaCompromiso(),
                c.getFechaLiberacion()
        );
    }
}
