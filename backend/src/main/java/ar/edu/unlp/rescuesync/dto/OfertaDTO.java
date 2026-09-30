package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.Oferta;

import java.time.LocalDateTime;
import java.util.List;

// Una oferta vista desde afuera.
// soyLider / miAporte se calculan para la ONG que consulta (null si consulta el coordinador o Bonita).
public record OfertaDTO(
        Long id,
        Long loteId,
        Long emergenciaId,
        String recurso,
        int cantidadRequerida,      // lo que pide el lote
        int total,                  // suma de aportes confirmados
        String estado,
        int version,
        boolean consorcio,
        Long liderId,
        String liderNombre,
        LocalDateTime fechaActualizacion,
        List<AporteDTO> aportes,
        Boolean soyLider,
        AporteDTO miAporte
) {
    // ongId = ONG que consulta (null = coordinador / Bonita)
    public static OfertaDTO desde(Oferta o, Long ongId) {
        AporteDTO mio = ongId == null ? null : o.aporteDe(ongId).map(AporteDTO::desde).orElse(null);
        return new OfertaDTO(
                o.getId(),
                o.getLote().getId(),
                o.getLote().getEmergencia().getId(),
                o.getRecurso(),
                o.getLote().getCantidad(),
                o.getCantidad(),
                o.getEstado().name(),
                o.getNumeroVersion() == null ? 0 : o.getNumeroVersion(),
                o.esConsorcio(),
                o.getOng().getId(),
                o.getOng().getNombre(),
                o.getFechaActualizacion(),
                o.getAportes().stream().map(AporteDTO::desde).toList(),
                ongId == null ? null : o.esLider(ongId),
                mio
        );
    }
}
