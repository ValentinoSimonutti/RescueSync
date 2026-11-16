package ar.edu.unlp.rescuesync.sistemanacional;

import java.time.LocalDateTime;
import java.util.List;

// Respuestas de los GET del Sistema Nacional que usa el dashboard del auditor.
// Solo se declaran los campos que se usan (Jackson ignora el resto).
public final class ConsultasSN {

    private ConsultasSN() {
    }

    // GET /api/recursos/disponibilidad -> un tipo de recurso, sumando las ONGs habilitadas
    public record Disponibilidad(String tipoRecurso, String nombre, String unidad,
                                 int total, int comprometido, int disponible) {
    }

    // GET /api/compromisos -> recursos bloqueados (ACTIVO) o ya devueltos (LIBERADO)
    public record Compromiso(String emergenciaRef, String cuit, String ongNombre, String tipoRecurso,
                             int cantidad, String rolAsignado, String estado,
                             LocalDateTime fechaCompromiso, LocalDateTime fechaLiberacion) {
    }

    // GET /api/ongs -> ONGs registradas en el padrón nacional
    public record Ong(String cuit, String nombre, String provincia, String estado,
                      List<String> certificacionesVigentes) {
    }
}
