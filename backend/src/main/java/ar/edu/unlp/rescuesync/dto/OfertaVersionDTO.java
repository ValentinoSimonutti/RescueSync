package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.OfertaVersion;

import java.time.LocalDateTime;

// Una línea del historial de una oferta
public record OfertaVersionDTO(int numero, LocalDateTime fecha, String autor, String accion, int total, String detalle) {

    public static OfertaVersionDTO desde(OfertaVersion v) {
        return new OfertaVersionDTO(v.getNumero(), v.getFecha(), v.getAutor(), v.getAccion(), v.getTotal(), v.getDetalle());
    }
}
