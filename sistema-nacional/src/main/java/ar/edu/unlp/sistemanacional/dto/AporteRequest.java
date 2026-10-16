package ar.edu.unlp.sistemanacional.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Lo que pone una ONG dentro de una oferta (oferta individual = 1 aporte, consorcio = varios)
public record AporteRequest(@NotBlank String cuit, @Min(1) int cantidad) {
}
