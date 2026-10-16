package ar.edu.unlp.rescuesync.sistemanacional;

// Un tipo de recurso del catálogo nacional (misma forma que devuelve la API).
// Ej: { codigo: "PARAMEDICO", nombre: "Paramédicos", categoria: "PERSONAL", unidad: "persona",
//       certificacionRequerida: "PRIMEROS_AUXILIOS" }
public record TipoRecursoDTO(
        String codigo,
        String nombre,
        String categoria,
        String unidad,
        String certificacionRequerida
) {
}
