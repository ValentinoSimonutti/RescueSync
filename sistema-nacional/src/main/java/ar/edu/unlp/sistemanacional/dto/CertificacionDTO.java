package ar.edu.unlp.sistemanacional.dto;

import java.util.List;

// Certificación del catálogo + códigos de los tipos de recurso que habilita
public record CertificacionDTO(
        String codigo,
        String nombre,
        String descripcion,
        List<String> habilitaRecursos
) {
}
