package ar.edu.unlp.rescuesync.dto;

// Respuesta a Bonita después de guardar la validación del Sistema Nacional.
// lotesCubiertos decide la compuerta: true si cada lote tiene al menos una oferta adjudicable.
public record ResultadoValidacionDTO(
        Long emergenciaId,
        boolean lotesCubiertos,
        int ofertasValidadas,
        int ofertasAdjudicables
) {
}
