package ar.edu.unlp.rescuesync.dto;

import java.time.LocalDateTime;
import java.util.List;

// Indicadores que salen de la API REST de Bonita (casos y tareas ejecutadas).
// Las duraciones van en minutos; el front las muestra en s / min / h / días.
public record DashboardBonitaDTO(
        int casosActivos,
        int casosFinalizados,
        Double duracionPromedioCasoMin,
        List<Etapa> etapas,
        List<Tarea> tareas,
        List<Pendiente> pendientes,
        List<Caso> casos,
        String advertencia          // ej: el proceso no está desplegado
) {

    // Una de las 7 etapas del enunciado: suma de sus tareas por caso, promediada entre casos
    public record Etapa(int orden, String nombre, long casos, Double promedioMin, Double minimoMin, Double maximoMin) {
    }

    // Una tarea del diagrama (humana o de servicio)
    public record Tarea(String nombre, String etapa, String responsable, String tipo,
                        long ejecuciones, Double promedioMin, Double maximoMin) {
    }

    // Tareas humanas esperando ahora mismo: muestra dónde se acumula el trabajo
    public record Pendiente(String tarea, String responsable, long cantidad, Double esperaPromedioMin) {
    }

    public record Caso(long caseId, Long emergenciaId, String zona, String estado,
                       LocalDateTime inicio, LocalDateTime fin, Double duracionMin, String etapaActual) {
    }
}
