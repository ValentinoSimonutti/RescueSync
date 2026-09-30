package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.dto.CoberturaDTO;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import ar.edu.unlp.rescuesync.service.OfertaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Endpoints que llama BONITA (no un usuario). Directiva 3 del enunciado:
// cuando vence el timer, el conector REST de "Obtener ofertas" hace un GET acá
// y recibe el JSON consolidado de todas las ofertas de la emergencia.
@RestController
@RequestMapping("/api/bonita")
public class BonitaIntegracionController {

    private final OfertaService ofertaService;
    private final EmergenciaService emergenciaService;
    private final String apiKey;

    public BonitaIntegracionController(OfertaService ofertaService,
                                       EmergenciaService emergenciaService,
                                       @Value("${bonita.integration-key}") String apiKey) {
        this.ofertaService = ofertaService;
        this.emergenciaService = emergenciaService;
        this.apiKey = apiKey;
    }

    // GET /api/bonita/emergencias/{id}/ofertas   header: X-Api-Key: <clave del yaml>
    // Bonita no tiene sesión de usuario: se identifica con una clave compartida.
    @GetMapping("/emergencias/{id}/ofertas")
    public CoberturaDTO ofertasConsolidadas(@PathVariable Long id,
                                            @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        if (clave == null || !clave.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave de integración inválida");
        }

        // La ventana ya cerró (por eso llama Bonita): la emergencia pasa a EN_EVALUACION
        emergenciaService.marcarEnEvaluacionSiCerro(id);

        return ofertaService.cobertura(id);
    }
}
