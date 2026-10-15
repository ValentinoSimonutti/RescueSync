package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.CertificacionDTO;
import ar.edu.unlp.sistemanacional.dto.DisponibilidadDTO;
import ar.edu.unlp.sistemanacional.dto.TipoRecursoDTO;
import ar.edu.unlp.sistemanacional.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo y consulta", description = "Certificaciones, tipos de recurso y disponibilidad global")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/catalogo/certificaciones")
    @Operation(summary = "Certificaciones del catálogo nacional y qué recursos habilita cada una")
    public List<CertificacionDTO> certificaciones() {
        return catalogoService.certificaciones();
    }

    @GetMapping("/catalogo/tipos-recurso")
    @Operation(summary = "Tipos de recurso (los códigos que usa la app local en sus lotes)")
    public List<TipoRecursoDTO> tiposRecurso() {
        return catalogoService.tiposRecurso();
    }

    @GetMapping("/recursos/disponibilidad")
    @Operation(summary = "Disponibilidad global de recursos (solo ONGs habilitadas). Filtro opcional por tipo")
    public List<DisponibilidadDTO> disponibilidad(@RequestParam(required = false) String tipoRecurso) {
        return catalogoService.disponibilidad(tipoRecurso);
    }
}
