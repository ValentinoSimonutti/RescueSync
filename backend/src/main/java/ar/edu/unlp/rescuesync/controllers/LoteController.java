package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.service.LoteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @PostMapping
    public Lote crear(
            @RequestBody Lote lote,
            @RequestParam Long emergenciaId) {

        return loteService.crearLote(lote, emergenciaId);

    }
}
