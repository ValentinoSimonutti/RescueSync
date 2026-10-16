package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.ValidacionRequest;
import ar.edu.unlp.sistemanacional.dto.ValidacionResponse;
import ar.edu.unlp.sistemanacional.service.ValidacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validaciones")
@Tag(name = "Validación por niveles de competencia")
public class ValidacionController {

    private final ValidacionService validacionService;

    public ValidacionController(ValidacionService validacionService) {
        this.validacionService = validacionService;
    }

    @PostMapping("/ofertas")
    @PreAuthorize("hasAnyRole('SISTEMA', 'ADMIN')")
    @Operation(summary = "Evalúa las ofertas de una emergencia y devuelve el perfil de competencia de cada aporte "
            + "(PRINCIPAL / APOYO_SECUNDARIO / SIN_HABILITACION_VIGENTE). No bloquea recursos")
    public ValidacionResponse validar(@Valid @RequestBody ValidacionRequest request) {
        return validacionService.validar(request);
    }
}
