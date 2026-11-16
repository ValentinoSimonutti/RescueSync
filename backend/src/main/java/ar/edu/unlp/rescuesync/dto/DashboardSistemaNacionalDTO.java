package ar.edu.unlp.rescuesync.dto;

import java.time.LocalDateTime;
import java.util.List;

// Indicadores que salen de la API del Sistema Nacional (en la nube): recursos a nivel país.
public record DashboardSistemaNacionalDTO(
        List<Recurso> recursos,
        int compromisosActivos,
        int compromisosLiberados,
        int unidadesComprometidas,
        int ongsHabilitadas,
        int ongsRegistradas,
        List<DashboardLocalDTO.Conteo> ongsPorEstado,
        List<CompromisoActivo> detalleActivos
) {

    // total = inventario declarado por las ONGs habilitadas
    public record Recurso(String codigo, String nombre, String unidad, int total, int comprometido, int disponible) {
    }

    public record CompromisoActivo(String emergenciaRef, String ong, String recurso, int cantidad,
                                   String rol, LocalDateTime fecha) {
    }
}
