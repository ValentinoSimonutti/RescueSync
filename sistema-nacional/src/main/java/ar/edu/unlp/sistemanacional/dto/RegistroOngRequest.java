package ar.edu.unlp.sistemanacional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Onboarding: la ONG se da de alta junto con el usuario de su representante.
// Queda PENDIENTE hasta que un ADMIN la habilite.
public record RegistroOngRequest(
        @Schema(example = "20-12345678-6") @NotBlank String cuit,
        @Schema(example = "Cruz Solidaria Pampeana") @NotBlank @Size(max = 150) String nombre,
        @Schema(example = "contacto@cruzpampeana.org") @NotBlank @Email String email,
        @Schema(example = "La Pampa") @NotBlank String provincia,
        @Schema(example = "cruz-pampeana") @NotBlank @Size(min = 3, max = 50) String username,
        @Schema(example = "secreta123") @NotBlank @Size(min = 6, max = 100) String password
) {
}
