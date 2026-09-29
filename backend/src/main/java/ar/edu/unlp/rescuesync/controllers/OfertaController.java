package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.service.OfertaService;
import org.springframework.web.bind.annotation.*;
import ar.edu.unlp.rescuesync.dto.NuevaOfertaDTO;
import ar.edu.unlp.rescuesync.dto.OfertaDTO;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @PostMapping
    public OfertaDTO crear(
            @RequestBody NuevaOfertaDTO dto,
            @RequestParam Long ongId,
            @RequestParam Long loteId) {

        return ofertaService.crearOferta(dto, ongId, loteId);
    }

}
