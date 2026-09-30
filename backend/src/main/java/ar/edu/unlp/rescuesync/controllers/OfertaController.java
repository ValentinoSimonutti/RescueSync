package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.NuevaOfertaDTO;
import ar.edu.unlp.rescuesync.dto.OfertaDTO;
import ar.edu.unlp.rescuesync.service.OfertaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;
    private final SesionActual sesionActual;

    public OfertaController(OfertaService ofertaService, SesionActual sesionActual) {
        this.ofertaService = ofertaService;
        this.sesionActual = sesionActual;
    }

    // POST /api/ofertas   body: {"loteId": 3, "cantidad": 2}
    // Antes recibía ongId por parámetro: cualquiera podía ofertar en nombre de cualquier ONG.
    // Ahora la ONG sale de la sesión.
    @PostMapping
    public OfertaDTO crear(@RequestBody NuevaOfertaDTO dto, HttpSession session) {

        UsuarioLogueado usuario = sesionActual.obtener(session);   // 401 si no hay sesión

        // Solo un representante de ONG puede ofertar (403 si no)
        if (usuario.rol() != Rol.REPRESENTANTE_ONG) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo una ONG puede ofrecer ayuda");
        }

        return ofertaService.crearOferta(dto, usuario);
    }
}
