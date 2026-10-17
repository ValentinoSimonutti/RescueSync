package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Pantalla "Adjudicar y notificar": por cada lote, las ofertas que el municipio puede elegir
// (validadas por el Sistema Nacional y que cubren el lote completo)
public record AdjudicacionDTO(
        EmergenciaDTO emergencia,
        List<LoteAdjudicable> lotes
) {
    public record LoteAdjudicable(Long loteId, String recurso, String recursoCodigo, int requerido,
                                  List<OfertaDTO> ofertas) {
    }
}
