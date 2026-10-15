package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.Ong;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Resumen de una ONG para listados: con los códigos de sus certificaciones vigentes
public record OngDTO(
        String cuit,
        String nombre,
        String email,
        String provincia,
        String estado,
        LocalDateTime fechaAlta,
        List<String> certificacionesVigentes
) {
    public static OngDTO desde(Ong o) {
        LocalDate hoy = LocalDate.now();
        return new OngDTO(
                o.getCuit(),
                o.getNombre(),
                o.getEmail(),
                o.getProvincia(),
                o.getEstado().name(),
                o.getFechaAlta(),
                o.getCertificaciones().stream()
                        .filter(c -> c.estaVigente(hoy))
                        .map(c -> c.getCertificacion().getCodigo())
                        .toList()
        );
    }
}
