package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.service.LoteService;
import org.springframework.web.bind.annotation.*;
import ar.edu.unlp.rescuesync.dto.LoteDTO;
import ar.edu.unlp.rescuesync.dto.NuevoLoteDTO;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @PostMapping
    public LoteDTO crear(
            @RequestBody NuevoLoteDTO dto,
            @RequestParam Long emergenciaId) {

        return loteService.crearLote(dto, emergenciaId);
    }
}
