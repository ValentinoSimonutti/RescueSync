package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.OngCertificacion;

import java.time.LocalDate;

public record CertificacionOngDTO(
        String codigo,
        String nombre,
        String nivel,
        LocalDate fechaOtorgamiento,
        LocalDate vigenteHasta,
        boolean vigente
) {
    public static CertificacionOngDTO desde(OngCertificacion c) {
        return new CertificacionOngDTO(
                c.getCertificacion().getCodigo(),
                c.getCertificacion().getNombre(),
                c.getNivel().name(),
                c.getFechaOtorgamiento(),
                c.getVigenteHasta(),
                c.estaVigente(LocalDate.now())
        );
    }
}
