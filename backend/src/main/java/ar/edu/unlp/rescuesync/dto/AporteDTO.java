package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.Aporte;

// Lo que pone una ONG dentro de una oferta
public record AporteDTO(Long ongId, String ongNombre, int cantidad, String estado) {

    public static AporteDTO desde(Aporte a) {
        return new AporteDTO(a.getOng().getId(), a.getOng().getNombre(), a.getCantidad(), a.getEstado().name());
    }
}
