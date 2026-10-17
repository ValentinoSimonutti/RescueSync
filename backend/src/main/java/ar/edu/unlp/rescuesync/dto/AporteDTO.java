package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.Aporte;

// Lo que pone una ONG dentro de una oferta
// rolValidacion / cantidadValidada: perfil del Sistema Nacional para esta ONG (null antes de validar)
public record AporteDTO(Long ongId, String ongNombre, int cantidad, String estado,
                        String rolValidacion, Integer cantidadValidada) {

    public static AporteDTO desde(Aporte a) {
        return new AporteDTO(a.getOng().getId(), a.getOng().getNombre(), a.getCantidad(), a.getEstado().name(),
                a.getRolValidacion() == null ? null : a.getRolValidacion().name(), a.getCantidadValidada());
    }
}
