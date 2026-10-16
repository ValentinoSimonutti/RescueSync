package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.CompromisoDTO;
import ar.edu.unlp.sistemanacional.dto.CompromisoRequest;
import ar.edu.unlp.sistemanacional.dto.CompromisoResponse;
import ar.edu.unlp.sistemanacional.dto.VerificacionResponse;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoCompromiso;
import ar.edu.unlp.sistemanacional.entity.enums.Rol;
import ar.edu.unlp.sistemanacional.security.UsuarioAutenticado;
import ar.edu.unlp.sistemanacional.service.CompromisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compromisos")
@Tag(name = "Compromiso de recursos", description = "Bloqueo transaccional de los recursos adjudicados")
public class CompromisoController {

    private final CompromisoService compromisoService;

    public CompromisoController(CompromisoService compromisoService) {
        this.compromisoService = compromisoService;
    }

    @PostMapping("/verificar")
    @PreAuthorize("hasAnyRole('SISTEMA', 'ADMIN')")
    @Operation(summary = "Chequea las restricciones (habilitación y disponibilidad) sin bloquear nada")
    public VerificacionResponse verificar(@Valid @RequestBody CompromisoRequest request) {
        return compromisoService.verificar(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SISTEMA')")
    @Operation(summary = "Compromete (bloquea) los recursos de las ofertas adjudicadas. Todo o nada: "
            + "409 con el detalle si algún aporte no cumple. Idempotente por (emergencia, oferta, ONG)")
    public CompromisoResponse comprometer(@Valid @RequestBody CompromisoRequest request) {
        return compromisoService.comprometer(request);
    }

    @GetMapping
    @Operation(summary = "Lista compromisos. Filtros opcionales. Una ONG solo ve los suyos")
    public List<CompromisoDTO> buscar(@AuthenticationPrincipal UsuarioAutenticado yo,
                                      @RequestParam(required = false) String emergenciaRef,
                                      @RequestParam(required = false) String ongCuit,
                                      @RequestParam(required = false) EstadoCompromiso estado) {
        String cuit = yo.rol() == Rol.ONG ? yo.ongCuit() : ongCuit;
        return compromisoService.buscar(emergenciaRef, cuit, estado);
    }
}
