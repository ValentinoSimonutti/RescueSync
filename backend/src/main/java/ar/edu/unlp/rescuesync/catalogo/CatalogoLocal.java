package ar.edu.unlp.rescuesync.catalogo;

import ar.edu.unlp.rescuesync.sistemanacional.TipoRecursoDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

// Catálogo nacional de tipos de recurso, precargado en resources/catalogo-tipos-recurso.json.
// Es una copia del catálogo del Sistema Nacional: la app web no llama a la API en la nube.
@Component
public class CatalogoLocal {

    private final List<TipoRecursoDTO> tipos;

    public CatalogoLocal(JsonMapper jsonMapper) {
        try (InputStream in = new ClassPathResource("catalogo-tipos-recurso.json").getInputStream()) {
            this.tipos = List.of(jsonMapper.readValue(in, TipoRecursoDTO[].class));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer catalogo-tipos-recurso.json", e);
        }
    }

    public List<TipoRecursoDTO> tiposRecurso() {
        return tipos;
    }

    public Optional<TipoRecursoDTO> tipoRecurso(String codigo) {
        return tipos.stream().filter(tipo -> tipo.codigo().equals(codigo)).findFirst();
    }
}