package ar.edu.unlp.sistemanacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Una oferta de la app local (la manda Bonita tal como la recibe del backend local).
// ofertaRef / loteRef: ids en la app local. tipoRecurso: código del catálogo nacional.
public record OfertaRequest(
        @Schema(example = "OF-31") @NotBlank String ofertaRef,
        @Schema(example = "LT-7") String loteRef,
        @Schema(example = "BOTE_RESCATE") @NotBlank String tipoRecurso,
        @Schema(example = "4") @Min(0) Integer cantidadRequerida,
        @NotEmpty @Valid List<AporteRequest> aportes
) {
}
