package ar.edu.unlp.sistemanacional.dto;

import java.util.List;

// compromisos: los de las ofertas del request. yaExistian: cuántos ya estaban (reintento idempotente)
public record CompromisoResponse(
        String emergenciaRef,
        String estadoProyecto,
        int creados,
        int yaExistian,
        List<CompromisoDTO> compromisos
) {
}
