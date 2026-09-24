package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.service.OfertaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @PostMapping
    public Oferta crear(
            @RequestBody Oferta oferta,
            @RequestParam Long ongId,
            @RequestParam Long loteId) {

        return ofertaService.crearOferta(oferta, ongId, loteId);
    }

}
