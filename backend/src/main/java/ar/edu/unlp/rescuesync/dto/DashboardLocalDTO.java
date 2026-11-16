package ar.edu.unlp.rescuesync.dto;

import java.time.LocalDateTime;
import java.util.List;

// Indicadores que salen de la base local (emergencias, lotes, ofertas y aportes).
// El filtro desde/hasta se aplica sobre la fecha de registro de la emergencia.
public record DashboardLocalDTO(
        Resumen resumen,
        List<Conteo> porEstado,
        List<Conteo> porTipo,
        List<Conteo> porGravedad,
        List<MovilizadoMes> movilizadosPorMes,
        List<MovilizadoRecurso> movilizadosPorRecurso,
        Consorcios consorcios,
        List<ParticipacionOng> participacionOngs,
        List<Conteo> validaciones,
        List<ProyectoFinalizado> proyectosFinalizados
) {

    public record Resumen(
            int emergencias,
            int enCurso,
            int cerradas,
            int lotesAdjudicados,
            int unidadesMovilizadas,
            Double porcentajeConsorcio,     // null si no hay lotes adjudicados
            Double porcentajeCobertura,     // lotes adjudicados / lotes de emergencias que llegaron a adjudicarse
            Double duracionPromedioHoras,   // registro -> cierre, solo cerradas
            int ofertas,
            Double promedioVersiones        // trazabilidad: cuántas versiones tiene en promedio una oferta
    ) {
    }

    public record Conteo(String clave, long cantidad) {
    }

    // mes = "2026-11"
    public record MovilizadoMes(String mes, String recurso, int unidades) {
    }

    public record MovilizadoRecurso(String recurso, int unidades, int lotes) {
    }

    public record Consorcios(int lotesAdjudicados, int porConsorcio, int individuales, Double porcentaje) {
    }

    public record ParticipacionOng(String ong, int unidadesMovilizadas, int lotesIndividuales,
                                   int lotesEnConsorcio, int ofertasPresentadas) {
    }

    public record ProyectoFinalizado(Long emergenciaId, String zona, String tipo, String gravedad,
                                     LocalDateTime fechaRegistro, LocalDateTime fechaCierre, Double duracionHoras,
                                     int lotes, int unidades, int ongs, boolean coberturaParcial) {
    }
}
