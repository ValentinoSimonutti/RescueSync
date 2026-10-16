package ar.edu.unlp.sistemanacional.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Una oferta de la app local (la manda Bonita tal como la recibe del backend local).
// ofertaRef / loteRef: ids en la app local. tipoRecurso: código del catálogo nacional.
public record OfertaRequest(
        @NotBlank String ofertaRef,
        String loteRef,
        @NotBlank String tipoRecurso,
        @Min(0) Integer cantidadRequerida,
        @NotEmpty @Valid List<AporteRequest> aportes
) {
}
