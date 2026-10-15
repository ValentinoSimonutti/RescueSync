package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoOngRequest(@NotNull EstadoOng estado, String motivo) {
}
