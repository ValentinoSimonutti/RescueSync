package ar.edu.unlp.sistemanacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record ActualizarInventarioRequest(
        @Schema(example = "15") @Min(0) int cantidadTotal
) {
}
