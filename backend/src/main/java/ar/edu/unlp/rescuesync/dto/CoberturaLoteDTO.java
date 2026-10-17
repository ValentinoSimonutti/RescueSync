package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Cuánto se cubrió de un lote con las ofertas activas
public record CoberturaLoteDTO(
        Long loteId,
        String recurso,
        String recursoCodigo,
        int requerido,
        int ofrecido,          // la mejor oferta activa (las ofertas compiten, no se suman)
        int faltante,          // 0 si está cubierto
        int porcentaje,        // tope 100
        boolean cubierto,      // al menos una oferta cubre el lote sola
        int ofertasCompletas,  // cuántas ofertas cubren el lote (alternativas para el municipio)
        Integer validado,      // lo mejor que validó el Sistema Nacional (null si todavía no validó)
        boolean habilitado,    // hay una oferta validada que cubre el lote (se puede adjudicar)
        List<OfertaDTO> ofertas
) {
}
