package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;

import java.time.LocalDateTime;
import java.util.List;

public record ValidacionResponse(
        String emergenciaRef,
        NivelRiesgo gravedad,
        LocalDateTime fecha,
        List<OfertaValidada> ofertas
) {
}
