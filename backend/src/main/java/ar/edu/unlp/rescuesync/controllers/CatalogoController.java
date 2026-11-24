package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.catalogo.CatalogoLocal;
import ar.edu.unlp.rescuesync.sistemanacional.TipoRecursoDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catálogo nacional de tipos de recurso (copia local precargada)
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoLocal catalogo;
    private final SesionActual sesionActual;

    public CatalogoController(CatalogoLocal catalogo, SesionActual sesionActual) {
        this.catalogo = catalogo;
        this.sesionActual = sesionActual;
    }

    // GET /api/catalogo/tipos-recurso -> tipos de recurso para armar los lotes
    @GetMapping("/tipos-recurso")
    public List<TipoRecursoDTO> tiposRecurso(HttpSession session) {
        sesionActual.obtener(session);   // 401 si no hay sesión
        return catalogo.tiposRecurso();
    }
}