package ar.edu.unlp.sistemanacional.dto;

import jakarta.validation.constraints.Min;

public record ActualizarInventarioRequest(@Min(0) int cantidadTotal) {
}
