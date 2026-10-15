package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.*;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import ar.edu.unlp.sistemanacional.security.UsuarioAutenticado;
import ar.edu.unlp.sistemanacional.service.OngService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ongs")
@Tag(name = "ONGs", description = "Onboarding, consulta y gestión de ONGs")
public class OngController {

    private final OngService ongService;

    public OngController(OngService ongService) {
        this.ongService = ongService;
    }

    // ---------- Público ----------

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Onboarding: registra una ONG y el usuario de su representante (queda PENDIENTE)")
    public OngDetalleDTO registrar(@Valid @RequestBody RegistroOngRequest request) {
        return ongService.registrar(request);
    }

    // ---------- Cualquier usuario autenticado ----------

    @GetMapping
    @Operation(summary = "Lista ONGs. Filtros opcionales: estado, certificación vigente y tipo de recurso disponible")
    public List<OngDTO> listar(@RequestParam(required = false) EstadoOng estado,
                               @RequestParam(required = false) String certificacion,
                               @RequestParam(required = false) String tipoRecurso) {
        return ongService.listar(estado, certificacion, tipoRecurso);
    }

    @GetMapping("/{cuit}")
    @Operation(summary = "Perfil completo de una ONG (certificaciones e inventario)")
    public OngDetalleDTO detalle(@PathVariable String cuit) {
        return ongService.detalle(cuit);
    }

    // ---------- ADMIN ----------

    @PatchMapping("/{cuit}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Habilita o suspende una ONG (ADMIN)")
    public OngDetalleDTO cambiarEstado(@PathVariable String cuit, @Valid @RequestBody CambioEstadoOngRequest request) {
        return ongService.cambiarEstado(cuit, request);
    }

    @PostMapping("/{cuit}/certificaciones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Otorga o renueva una certificación a una ONG (ADMIN)")
    public OngDetalleDTO otorgarCertificacion(@PathVariable String cuit,
                                              @Valid @RequestBody OtorgarCertificacionRequest request) {
        return ongService.otorgarCertificacion(cuit, request);
    }

    // ---------- La propia ONG ----------

    @GetMapping("/me")
    @PreAuthorize("hasRole('ONG')")
    @Operation(summary = "Perfil de mi ONG (rol ONG)")
    public OngDetalleDTO miOng(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ongService.detalle(yo.ongCuit());
    }

    @PutMapping("/me/inventario/{tipoRecurso}")
    @PreAuthorize("hasRole('ONG')")
    @Operation(summary = "Declara cuánto tiene mi ONG de un tipo de recurso (rol ONG)")
    public InventarioDTO actualizarInventario(@AuthenticationPrincipal UsuarioAutenticado yo,
                                              @PathVariable String tipoRecurso,
                                              @Valid @RequestBody ActualizarInventarioRequest request) {
        return ongService.actualizarInventario(yo.ongCuit(), tipoRecurso, request.cantidadTotal());
    }
}
