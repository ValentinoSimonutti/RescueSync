package ar.edu.unlp.rescuesync.sistemanacional;

import java.util.List;

// Contrato de POST /api/validaciones/ofertas del Sistema Nacional.
// El backend arma el request (Bonita lo recibe en el GET de ofertas y se lo manda al SN)
// y después recibe la respuesta del SN (Bonita la reenvía a /api/bonita/emergencias/{id}/validaciones).
// Las referencias que viajan son EMG-<id>, OF-<id> y LT-<id> (ids de esta base).
public final class ValidacionSN {

    private ValidacionSN() {
    }

    public record Request(String emergenciaRef, String gravedad, String tipoDesastre, List<OfertaRequest> ofertas) {
    }

    public record OfertaRequest(String ofertaRef, String loteRef, String tipoRecurso, int cantidadRequerida,
                                List<AporteRequest> aportes) {
    }

    public record AporteRequest(String cuit, int cantidad) {
    }

    // Respuesta: solo se declaran los campos que se usan (Jackson ignora el resto)
    public record Response(String emergenciaRef, List<OfertaValidada> ofertas) {
    }

    public record OfertaValidada(String ofertaRef, int cantidadValidada, String rolOferta,
                                 List<String> observaciones, List<Perfil> aportes) {
    }

    public record Perfil(String cuit, int cantidadValidable, String rolAsignado) {
    }

    public static String emergenciaRef(Long id) { return "EMG-" + id; }
    public static String ofertaRef(Long id) { return "OF-" + id; }
    public static String loteRef(Long id) { return "LT-" + id; }

    // "OF-12" -> 12 (null si no tiene esa forma)
    public static Long idDe(String ref, String prefijo) {
        if (ref == null || !ref.startsWith(prefijo + "-")) return null;
        try {
            return Long.valueOf(ref.substring(prefijo.length() + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
