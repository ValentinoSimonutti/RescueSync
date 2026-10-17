package ar.edu.unlp.rescuesync.sistemanacional;

import java.util.List;

// Contrato de POST /api/compromisos y POST /api/proyectos/{ref}/cierre del Sistema Nacional.
// El backend arma el request (Bonita lo pide en "Comprometer Recursos" y se lo manda al SN tal cual)
// y después recibe lo que respondió el SN: 201 con los compromisos o 409 con el motivo de cada aporte.
public final class CompromisoSN {

    private CompromisoSN() {
    }

    // Las ofertas tienen la misma forma que en la validación
    public record Request(String emergenciaRef, String gravedad, String descripcion,
                          List<ValidacionSN.OfertaRequest> ofertas) {
    }

    // 201 -> trae compromisos. 409 -> es un ProblemDetail (status, detail) y, si fue por recursos, items.
    // Solo se declaran los campos que se usan (Jackson ignora el resto)
    public record Respuesta(Integer status, String detail, List<Item> items, List<Object> compromisos) {
    }

    // Chequeo de un aporte: ok = false -> motivo explica qué restricción no cumplió
    public record Item(String ofertaRef, String cuit, String tipoRecurso, int cantidad, int disponible,
                       boolean ok, String motivo) {
    }

    // Respuesta de la liberación / cierre
    public record Cierre(String emergenciaRef, String estadoProyecto) {
    }
}
