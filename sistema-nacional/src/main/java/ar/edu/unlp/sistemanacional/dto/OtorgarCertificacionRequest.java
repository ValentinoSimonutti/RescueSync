package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Si la ONG ya tenía la certificación, se renueva (nivel y vencimiento nuevos)
public record OtorgarCertificacionRequest(
        @Schema(example = "PRIMEROS_AUXILIOS") @NotBlank String certificacion,
        @Schema(example = "INTERMEDIO") @NotNull NivelCompetencia nivel,
        @Schema(example = "2027-12-31") @NotNull LocalDate vigenteHasta
) {
}
