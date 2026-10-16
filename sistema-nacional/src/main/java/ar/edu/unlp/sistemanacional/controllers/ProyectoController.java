package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.LiberacionResponse;
import ar.edu.unlp.sistemanacional.dto.ProyectoDTO;
import ar.edu.unlp.sistemanacional.service.CompromisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos")
@Tag(name = "Liberación y cierre", description = "Finalización de actividades y cierre de proyectos")
public class ProyectoController {

    private final CompromisoService compromisoService;

    public ProyectoController(CompromisoService compromisoService) {
        this.compromisoService = compromisoService;
    }

    @GetMapping("/{emergenciaRef}")
    @PreAuthorize("hasAnyRole('SISTEMA', 'ADMIN')")
    @Operation(summary = "Estado del proyecto de una emergencia y todos sus compromisos")
    public ProyectoDTO proyecto(@PathVariable String emergenciaRef) {
        return compromisoService.proyecto(emergenciaRef);
    }

    @PostMapping("/{emergenciaRef}/ongs/{cuit}/finalizacion")
    @PreAuthorize("hasRole('SISTEMA')")
    @Operation(summary = "La ONG terminó sus actividades: libera sus recursos bloqueados en esta emergencia")
    public LiberacionResponse finalizarOng(@PathVariable String emergenciaRef, @PathVariable String cuit) {
        return compromisoService.finalizarOng(emergenciaRef, cuit);
    }

    @PostMapping("/{emergenciaRef}/cierre")
    @PreAuthorize("hasRole('SISTEMA')")
    @Operation(summary = "Cierra el proyecto: libera todo lo que siga bloqueado y no admite más compromisos")
    public LiberacionResponse cerrar(@PathVariable String emergenciaRef) {
        return compromisoService.cerrarProyecto(emergenciaRef);
    }
}
