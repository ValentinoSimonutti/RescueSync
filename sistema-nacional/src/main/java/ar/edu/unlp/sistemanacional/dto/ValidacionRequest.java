package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ValidacionRequest(
        @Schema(example = "EMG-12") @NotBlank String emergenciaRef,
        @Schema(example = "ALTA") @NotNull NivelRiesgo gravedad,
        @Schema(example = "Inundación") String tipoDesastre,
        @NotNull @Valid List<OfertaRequest> ofertas   // vacía = no hubo ofertas completas: devuelve vacío
) {
}
