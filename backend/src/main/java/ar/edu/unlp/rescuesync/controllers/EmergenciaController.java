package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.service.EmergenciaService;
import org.springframework.web.bind.annotation.*;
import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;

import java.util.List;

@RestController
@RequestMapping("/api/emergencias")
public class EmergenciaController {

    private final EmergenciaService emergenciaService;

    public EmergenciaController(EmergenciaService emergenciaService) {
        this.emergenciaService = emergenciaService;
    }

    @PostMapping
    public EmergenciaDTO registrar(@RequestBody NuevaEmergenciaDTO dto) {
        return emergenciaService.registrarEmergencia(dto);
    }

    @GetMapping
    public List<EmergenciaDTO> listar() {
        return emergenciaService.listarEmergencias();
    }

    @GetMapping("/{id}")
    public EmergenciaDTO buscar(@PathVariable Long id) {
        return emergenciaService.buscarPorId(id);
    }
}