package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergencias")
public class EmergenciaController {

    private final EmergenciaService emergenciaService;

    public EmergenciaController(EmergenciaService emergenciaService) {
        this.emergenciaService = emergenciaService;
    }

    @PostMapping
    public Emergencia registrar(@RequestBody Emergencia emergencia) {
        return emergenciaService.registrarEmergencia(emergencia);
    }

    @GetMapping
    public List<Emergencia> listar() {
        return emergenciaService.listarEmergencias();
    }

    @GetMapping("/{id}")
    public Emergencia buscar(@PathVariable Long id) {
        return emergenciaService.buscarPorId(id);
    }
}