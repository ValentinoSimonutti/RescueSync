package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.*;
import ar.edu.unlp.rescuesync.service.OfertaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Ofertas de las ONGs. La ONG SIEMPRE sale de la sesión (nunca de un parámetro).
@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;
    private final SesionActual sesionActual;

    public OfertaController(OfertaService ofertaService, SesionActual sesionActual) {
        this.ofertaService = ofertaService;
        this.sesionActual = sesionActual;
    }

    // POST /api/ofertas   body: {"loteId": 3, "cantidad": 2, "ongsInvitadas": [5]}
    // Crea una oferta individual o, si invita ONGs, un consorcio
    @PostMapping
    public OfertaDTO crear(@RequestBody NuevaOfertaDTO dto, HttpSession session) {
        return ofertaService.crearOferta(dto, ong(session));
    }

    // GET /api/ofertas/mias -> ofertas donde participa la ONG + invitaciones pendientes
    @GetMapping("/mias")
    public List<OfertaDTO> mias(HttpSession session) {
        return ofertaService.misOfertas(ong(session));
    }

    // PUT /api/ofertas/{id}/aporte   body: {"cantidad": 3}
    // Modifica el aporte de la ONG (o acepta una invitación a consorcio). Genera versión nueva.
    @PutMapping("/{id}/aporte")
    public OfertaDTO actualizarAporte(@PathVariable Long id, @RequestBody ActualizarAporteDTO dto,
                                      HttpSession session) {
        return ofertaService.actualizarAporte(id, dto.cantidad(), ong(session));
    }

    // POST /api/ofertas/{id}/retirar -> la líder retira la oferta; una invitada rechaza o sale del consorcio
    @PostMapping("/{id}/retirar")
    public OfertaDTO retirar(@PathVariable Long id, HttpSession session) {
        return ofertaService.retirar(id, ong(session));
    }

    // GET /api/ofertas/{id}/historial -> versiones (ONG participante o coordinador)
    @GetMapping("/{id}/historial")
    public List<OfertaVersionDTO> historial(@PathVariable Long id, HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        boolean esCoordinador = usuario.rol() == Rol.CCR;
        if (!esCoordinador && usuario.rol() != Rol.REPRESENTANTE_ONG) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés ver este historial");
        }
        return ofertaService.historial(id, usuario, esCoordinador);
    }

    // GET /api/ofertas/ongs -> ONGs a las que se puede invitar a un consorcio
    @GetMapping("/ongs")
    public List<OngDTO> ongsParaInvitar(HttpSession session) {
        return ofertaService.ongsParaInvitar(ong(session));
    }

    // Sesión obligatoria (401) y rol ONG (403)
    private UsuarioLogueado ong(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        if (usuario.rol() != Rol.REPRESENTANTE_ONG) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo una ONG puede gestionar ofertas");
        }
        return usuario;
    }
}
