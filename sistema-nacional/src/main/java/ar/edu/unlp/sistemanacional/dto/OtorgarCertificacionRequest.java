package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Si la ONG ya tenía la certificación, se renueva (nivel y vencimiento nuevos)
public record OtorgarCertificacionRequest(
        @NotBlank String certificacion,
        @NotNull NivelCompetencia nivel,
        @NotNull LocalDate vigenteHasta
) {
}
