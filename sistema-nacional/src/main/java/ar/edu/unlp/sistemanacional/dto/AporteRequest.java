package ar.edu.unlp.sistemanacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Lo que pone una ONG dentro de una oferta (oferta individual = 1 aporte, consorcio = varios)
public record AporteRequest(
        @Schema(example = "30712345671") @NotBlank String cuit,
        @Schema(example = "2") @Min(1) int cantidad
) {
}
