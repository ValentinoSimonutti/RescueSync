package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoOngRequest(
        @Schema(example = "HABILITADA") @NotNull EstadoOng estado,
        @Schema(example = "Documentación verificada") String motivo
) {
}
