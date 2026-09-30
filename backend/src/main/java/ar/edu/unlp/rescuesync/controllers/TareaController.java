package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.TareaDTO;
import ar.edu.unlp.rescuesync.service.TareaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService tareaService;
    private final SesionActual sesionActual;

    public TareaController(TareaService tareaService, SesionActual sesionActual) {
        this.tareaService = tareaService;
        this.sesionActual = sesionActual;
    }

    // GET /api/tareas -> la bandeja del usuario logueado
    @GetMapping
    public List<TareaDTO> misTareas(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);   // 401 si no hay sesión
        return tareaService.tareasDe(usuario);
    }
}