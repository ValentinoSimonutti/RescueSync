package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.sistemanacional.ValidacionSN;

import java.util.List;

// Lo que recibe Bonita cuando vence el timer (GET /api/bonita/emergencias/{id}/ofertas).
// validacionRequest va tal cual al Sistema Nacional (POST /api/validaciones/ofertas):
// solo lleva las ofertas que cubren su lote, con los aportes confirmados de cada ONG (por CUIT).
public record ConsolidadoBonitaDTO(
        Long emergenciaId,
        boolean lotesCubiertos,        // cobertura antes de validar (todas las ofertas activas)
        int totalOfertas,
        List<CoberturaLoteDTO> lotes,
        ValidacionSN.Request validacionRequest
) {
}
