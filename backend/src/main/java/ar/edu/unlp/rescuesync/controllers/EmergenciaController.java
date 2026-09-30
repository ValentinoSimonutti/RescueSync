package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.PublicarLotesDTO;

import java.util.List;

@RestController
@RequestMapping("/api/emergencias")
public class EmergenciaController {

    private final EmergenciaService emergenciaService;
    private final SesionActual sesionActual;       // NUEVO: para saber quién está logueado

    public EmergenciaController(EmergenciaService emergenciaService, SesionActual sesionActual) {
        this.emergenciaService = emergenciaService;
        this.sesionActual = sesionActual;
    }

    @PostMapping
    public EmergenciaDTO registrar(@RequestBody NuevaEmergenciaDTO dto, HttpSession session) {

        // Quién hace la acción (401 si no hay sesión)
        UsuarioLogueado usuario = sesionActual.obtener(session);

        // Solo un operador municipal puede registrar emergencias (403 si no)
        if (usuario.rol() != Rol.OPERADOR_MUNICIPAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un operador municipal puede registrar emergencias");
        }

        return emergenciaService.registrarEmergencia(dto, usuario);
    }

    // POST /api/emergencias/{id}/lotes -> el coordinador publica la convocatoria
    // @PathVariable toma el {id} de la URL; @RequestBody convierte el JSON en PublicarLotesDTO
    @PostMapping("/{id}/lotes")
    public EmergenciaDTO publicarLotes(@PathVariable Long id,
                                       @RequestBody PublicarLotesDTO dto,
                                       HttpSession session) {

        UsuarioLogueado usuario = sesionActual.obtener(session);   // 401 si no hay sesión

        // Solo el Centro Coordinador Regional arma los lotes (403 si no)
        if (usuario.rol() != Rol.CCR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el centro coordinador puede publicar lotes");
        }

        return emergenciaService.publicarLotes(id, dto, usuario);
    }

    @GetMapping
    public List<EmergenciaDTO> listar() {
        return emergenciaService.listarEmergencias();
    }

    @GetMapping("/{id}")
    public EmergenciaDTO buscar(@PathVariable Long id) {
        return emergenciaService.buscarPorId(id);
    }

    // GET /api/emergencias/mias -> emergencias del municipio logueado
    @GetMapping("/mias")
    public List<EmergenciaDTO> misEmergencias(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);

        if (usuario.rol() != Rol.OPERADOR_MUNICIPAL) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un operador municipal puede consultar sus emergencias"
            );
        }

        return emergenciaService.listarMisEmergencias(usuario);
    }
}