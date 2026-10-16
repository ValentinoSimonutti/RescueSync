package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.sistemanacional.SistemaNacionalClient;
import ar.edu.unlp.rescuesync.sistemanacional.TipoRecursoDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Proxy del catálogo nacional: el front no habla directo con el Sistema Nacional
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final SistemaNacionalClient sistemaNacional;
    private final SesionActual sesionActual;

    public CatalogoController(SistemaNacionalClient sistemaNacional, SesionActual sesionActual) {
        this.sistemaNacional = sistemaNacional;
        this.sesionActual = sesionActual;
    }

    // GET /api/catalogo/tipos-recurso -> tipos de recurso para armar los lotes
    @GetMapping("/tipos-recurso")
    public List<TipoRecursoDTO> tiposRecurso(HttpSession session) {
        sesionActual.obtener(session);   // 401 si no hay sesión
        return sistemaNacional.tiposRecurso();
    }
}
