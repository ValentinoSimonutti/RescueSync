package ar.edu.unlp.sistemanacional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Onboarding: la ONG se da de alta junto con el usuario de su representante.
// Queda PENDIENTE hasta que un ADMIN la habilite.
public record RegistroOngRequest(
        @NotBlank String cuit,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email String email,
        @NotBlank String provincia,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
