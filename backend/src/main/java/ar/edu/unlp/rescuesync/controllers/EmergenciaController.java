package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import ar.edu.unlp.rescuesync.service.OfertaService;
import ar.edu.unlp.rescuesync.service.AdjudicacionService;
import ar.edu.unlp.rescuesync.dto.AdjudicacionDTO;
import ar.edu.unlp.rescuesync.dto.AdjudicarDTO;
import ar.edu.unlp.rescuesync.dto.CoberturaDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.PublicarLotesDTO;
import ar.edu.unlp.rescuesync.dto.LoteDTO;

import java.util.List;

@RestController
@RequestMapping("/api/emergencias")
public class EmergenciaController {

    private final EmergenciaService emergenciaService;
    private final OfertaService ofertaService;     // para la cobertura de las convocatorias
    private final AdjudicacionService adjudicacionService;
    private final SesionActual sesionActual;       // para saber quién está logueado

    public EmergenciaController(EmergenciaService emergenciaService, OfertaService ofertaService,
                                AdjudicacionService adjudicacionService, SesionActual sesionActual) {
        this.emergenciaService = emergenciaService;
        this.ofertaService = ofertaService;
        this.adjudicacionService = adjudicacionService;
        this.sesionActual = sesionActual;
    }

    // GET /api/emergencias/convocatorias -> convocatorias abiertas (coordinador)
    @GetMapping("/convocatorias")
    public List<EmergenciaDTO> convocatorias(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        if (usuario.rol() != Rol.CCR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el centro coordinador puede ver las convocatorias");
        }
        return emergenciaService.listarConvocatoriasAbiertas();
    }

    // GET /api/emergencias/{id}/cobertura -> cuánto se ofreció por lote
    // Coordinador: para seguir la convocatoria. ONG: para ver cuánto falta y colaborar.
    @GetMapping("/{id}/cobertura")
    public CoberturaDTO cobertura(@PathVariable Long id, HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        if (usuario.rol() != Rol.CCR && usuario.rol() != Rol.REPRESENTANTE_ONG) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés ver la cobertura");
        }
        return ofertaService.cobertura(id);
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

    @GetMapping("/{id}/lotes")
    public List<LoteDTO> consultarLotes(
            @PathVariable("id") Long id,
            HttpSession session
    ) {
        UsuarioLogueado usuario = sesionActual.obtener(session);

        if (usuario.rol() != Rol.REPRESENTANTE_ONG) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo una ONG puede consultar las convocatorias disponibles"
            );
        }

        return emergenciaService.consultarLotes(id, usuario);
    }

    // GET /api/emergencias/{id}/adjudicacion -> ofertas validadas que el municipio puede elegir, por lote
    @GetMapping("/{id}/adjudicacion")
    public AdjudicacionDTO opcionesAdjudicacion(@PathVariable Long id, HttpSession session) {
        return adjudicacionService.opciones(id, municipio(session));
    }

    // POST /api/emergencias/{id}/adjudicacion -> adjudica una oferta por lote y completa la tarea en Bonita
    @PostMapping("/{id}/adjudicacion")
    public EmergenciaDTO adjudicar(@PathVariable Long id, @RequestBody AdjudicarDTO dto, HttpSession session) {
        return adjudicacionService.adjudicar(id, dto, municipio(session));
    }

    private UsuarioLogueado municipio(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        if (usuario.rol() != Rol.OPERADOR_MUNICIPAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el municipio puede adjudicar");
        }
        return usuario;
    }
}
