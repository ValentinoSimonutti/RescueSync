package ar.edu.unlp.sistemanacional.dto;

import java.util.List;

// cuit null = cierre del proyecto completo. liberados vacío = no quedaba nada activo (idempotente).
public record LiberacionResponse(
        String emergenciaRef,
        String cuit,
        String estadoProyecto,
        List<CompromisoDTO> liberados
) {
}
