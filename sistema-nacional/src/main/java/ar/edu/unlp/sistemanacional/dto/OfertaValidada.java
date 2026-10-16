package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;

import java.util.List;

// rolOferta: PRINCIPAL si al menos un aporte es principal (en un consorcio, el resto apoya);
// APOYO_SECUNDARIO si solo hay aportes de apoyo; SIN_HABILITACION_VIGENTE si ningún aporte participa.
public record OfertaValidada(
        String ofertaRef,
        String loteRef,
        String tipoRecurso,
        Integer cantidadRequerida,
        int cantidadOfrecida,
        int cantidadValidada,
        Boolean cubreRequerido,            // null si no se mandó cantidadRequerida
        RolAsignado rolOferta,
        List<String> observaciones,
        List<PerfilCompetencia> aportes
) {
}
