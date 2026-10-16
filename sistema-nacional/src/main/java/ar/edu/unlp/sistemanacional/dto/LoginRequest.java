package ar.edu.unlp.sistemanacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "bonita") @NotBlank String username,
        @Schema(example = "bonita123") @NotBlank String password
) {
}
