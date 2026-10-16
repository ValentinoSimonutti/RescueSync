package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Ofertas adjudicadas por el municipio: se bloquean todas o ninguna
public record CompromisoRequest(
        @Schema(example = "EMG-12") @NotBlank String emergenciaRef,
        @Schema(example = "ALTA") @NotNull NivelRiesgo gravedad,
        @Schema(example = "Inundación en Bahía Blanca") String descripcion,
        @NotEmpty @Valid List<OfertaRequest> ofertas
) {
}
