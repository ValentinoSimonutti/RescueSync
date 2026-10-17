package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Lo que elige el municipio: exactamente una oferta por lote
public record AdjudicarDTO(List<Seleccion> selecciones) {

    public record Seleccion(Long loteId, Long ofertaId) {
    }
}
