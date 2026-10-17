package ar.edu.unlp.rescuesync.dto;

import java.time.LocalDateTime;
import java.util.List;

// Etapa de ejecución: qué se adjudicó por lote y qué ONGs ya terminaron sus actividades.
public record DespliegueDTO(
        EmergenciaDTO emergencia,
        List<Asignacion> asignaciones,
        int ongsTotal,
        int ongsFinalizadas
) {
    // La oferta adjudicada de un lote
    public record Asignacion(Long ofertaId, Long loteId, String recurso, int cantidad, boolean consorcio,
                             List<Participante> ongs) {
    }

    // Una ONG dentro de esa oferta (rol: perfil del Sistema Nacional)
    public record Participante(Long ongId, String ongNombre, int cantidad, String rol,
                               LocalDateTime fechaFinalizacion) {
    }
}
