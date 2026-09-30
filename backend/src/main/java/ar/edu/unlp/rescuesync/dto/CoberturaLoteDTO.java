package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Cuánto se cubrió de un lote con las ofertas activas
public record CoberturaLoteDTO(
        Long loteId,
        String recurso,
        int requerido,
        int ofrecido,          // suma de las ofertas activas (puede superar lo requerido)
        int faltante,          // 0 si está cubierto
        int porcentaje,        // tope 100
        boolean cubierto,
        List<OfertaDTO> ofertas
) {
}
