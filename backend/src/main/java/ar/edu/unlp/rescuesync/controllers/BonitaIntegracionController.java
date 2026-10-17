package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.dto.ConsolidadoBonitaDTO;
import ar.edu.unlp.rescuesync.dto.ResultadoValidacionDTO;
import ar.edu.unlp.rescuesync.service.AdjudicacionService;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import ar.edu.unlp.rescuesync.sistemanacional.ValidacionSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Endpoints que llama BONITA (no un usuario). Bonita no tiene sesión de usuario:
// se identifica con una clave compartida en el header X-Api-Key.
//  1. Vence el timer -> "Obtener ofertas" pide el consolidado (directiva 3 del enunciado).
//  2. "Evaluar Ofertas" manda validacionRequest al Sistema Nacional y nos reenvía su respuesta.
@RestController
@RequestMapping("/api/bonita")
public class BonitaIntegracionController {

    private static final Logger log = LoggerFactory.getLogger(BonitaIntegracionController.class);

    private final AdjudicacionService adjudicacionService;
    private final EmergenciaService emergenciaService;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public BonitaIntegracionController(AdjudicacionService adjudicacionService,
                                       EmergenciaService emergenciaService,
                                       JsonMapper jsonMapper,
                                       @Value("${bonita.integration-key}") String apiKey) {
        this.adjudicacionService = adjudicacionService;
        this.emergenciaService = emergenciaService;
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    // GET /api/bonita/emergencias/{id}/ofertas   header: X-Api-Key: <clave del yaml>
    @GetMapping("/emergencias/{id}/ofertas")
    public ConsolidadoBonitaDTO ofertasConsolidadas(@PathVariable Long id,
                                                    @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        validarClave(clave);

        // La ventana ya cerró (por eso llama Bonita): la emergencia pasa a EN_EVALUACION
        emergenciaService.marcarEnEvaluacionSiCerro(id);

        return adjudicacionService.consolidadoParaBonita(id);
    }

    // POST /api/bonita/emergencias/{id}/validaciones   body: la respuesta de POST /api/validaciones/ofertas del SN
    // Devuelve lotesCubiertos con lo validado: es lo que decide la compuerta de Bonita.
    // Se lee como texto para poder loguear lo que llegó si no es una respuesta válida (ej. un error del SN).
    @PostMapping("/emergencias/{id}/validaciones")
    public ResultadoValidacionDTO registrarValidacion(@PathVariable Long id,
                                                      @RequestBody(required = false) String cuerpo,
                                                      @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        validarClave(clave);
        ValidacionSN.Response validacion = null;
        try {
            if (cuerpo != null && !cuerpo.isBlank()) validacion = jsonMapper.readValue(cuerpo, ValidacionSN.Response.class);
        } catch (JacksonException e) {
            // queda null y se rechaza abajo
        }
        if (validacion == null || validacion.ofertas() == null) {
            log.warn("Emergencia {}: Bonita no mandó una respuesta de validación del Sistema Nacional. Recibido: {}", id, cuerpo);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Falta la respuesta del Sistema Nacional. Se recibió: " + abreviar(cuerpo));
        }
        return adjudicacionService.registrarValidacion(id, validacion);
    }

    private static String abreviar(String texto) {
        if (texto == null || texto.isBlank()) return "(vacío)";
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }

    private void validarClave(String clave) {
        if (clave == null || !clave.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave de integración inválida");
        }
    }
}
