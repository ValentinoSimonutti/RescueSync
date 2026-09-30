package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Estado consolidado de las ofertas de una emergencia.
// Lo usan: la pantalla del coordinador y Bonita cuando vence el timer.
public record CoberturaDTO(
        EmergenciaDTO emergencia,
        boolean lotesCubiertos,        // true si TODOS los lotes están cubiertos -> compuerta de Bonita
        int totalOfertas,
        List<CoberturaLoteDTO> lotes
) {
}
