package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.dto.ConsolidadoBonitaDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.ResultadoCompromisoDTO;
import ar.edu.unlp.rescuesync.dto.ResultadoValidacionDTO;
import ar.edu.unlp.rescuesync.service.AdjudicacionService;
import ar.edu.unlp.rescuesync.service.EjecucionService;
import ar.edu.unlp.rescuesync.service.EmergenciaService;
import ar.edu.unlp.rescuesync.sistemanacional.CompromisoSN;
import ar.edu.unlp.rescuesync.sistemanacional.ValidacionSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Predicate;

// Endpoints que llama BONITA (no un usuario). Bonita no tiene sesión de usuario:
// se identifica con una clave compartida en el header X-Api-Key.
//  1. Vence el timer -> "Obtener ofertas" pide el consolidado (directiva 3 del enunciado).
//  2. "Evaluar Ofertas" manda validacionRequest al Sistema Nacional y nos reenvía su respuesta.
//  3. "Comprometer Recursos" pide el request de compromiso, lo manda al SN y nos reenvía la respuesta (201 o 409).
//  4. Al completarse "Ejecutar y marcar tareas finalizadas", Bonita cierra el proyecto en el SN y nos avisa.
@RestController
@RequestMapping("/api/bonita")
public class BonitaIntegracionController {

    private static final Logger log = LoggerFactory.getLogger(BonitaIntegracionController.class);

    private final AdjudicacionService adjudicacionService;
    private final EjecucionService ejecucionService;
    private final EmergenciaService emergenciaService;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public BonitaIntegracionController(AdjudicacionService adjudicacionService,
                                       EjecucionService ejecucionService,
                                       EmergenciaService emergenciaService,
                                       JsonMapper jsonMapper,
                                       @Value("${bonita.integration-key}") String apiKey) {
        this.adjudicacionService = adjudicacionService;
        this.ejecucionService = ejecucionService;
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
        ValidacionSN.Response validacion = leer(id, cuerpo, ValidacionSN.Response.class, v -> v.ofertas() != null);
        return adjudicacionService.registrarValidacion(id, validacion);
    }

    // GET /api/bonita/emergencias/{id}/compromiso -> body de POST /api/compromisos del SN (ofertas adjudicadas)
    @GetMapping("/emergencias/{id}/compromiso")
    public CompromisoSN.Request compromiso(@PathVariable Long id,
                                           @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        validarClave(clave);
        return ejecucionService.compromisoParaBonita(id);
    }

    // POST /api/bonita/emergencias/{id}/compromiso   body: la respuesta del SN, sea 201 o 409
    // Devuelve compromisoOk: es lo que decide la compuerta de Bonita (false -> volver a adjudicar).
    // Cualquier otra cosa (401, 500, vacío) se rechaza con 400 para que el conector falle.
    @PostMapping("/emergencias/{id}/compromiso")
    public ResultadoCompromisoDTO registrarCompromiso(@PathVariable Long id,
                                                      @RequestBody(required = false) String cuerpo,
                                                      @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        validarClave(clave);
        CompromisoSN.Respuesta respuesta = leer(id, cuerpo, CompromisoSN.Respuesta.class,
                r -> r.compromisos() != null || Integer.valueOf(409).equals(r.status()));
        return ejecucionService.registrarCompromiso(id, respuesta);
    }

    // POST /api/bonita/emergencias/{id}/cierre   body: la respuesta de POST /api/proyectos/{ref}/cierre del SN
    @PostMapping("/emergencias/{id}/cierre")
    public EmergenciaDTO registrarCierre(@PathVariable Long id,
                                         @RequestBody(required = false) String cuerpo,
                                         @RequestHeader(value = "X-Api-Key", required = false) String clave) {
        validarClave(clave);
        CompromisoSN.Cierre cierre = leer(id, cuerpo, CompromisoSN.Cierre.class, c -> c.estadoProyecto() != null);
        return ejecucionService.registrarCierre(id, cierre);
    }

    // Lee lo que reenvió Bonita. Si no es una respuesta esperada del SN (ej. un error 401), lo loguea
    // y responde 400 con lo que llegó, así se ve en el conector que falló.
    private <T> T leer(Long id, String cuerpo, Class<T> tipo, Predicate<T> esValida) {
        T valor = null;
        try {
            if (cuerpo != null && !cuerpo.isBlank()) valor = jsonMapper.readValue(cuerpo, tipo);
        } catch (JacksonException e) {
            // queda null y se rechaza abajo
        }
        if (valor == null || !esValida.test(valor)) {
            log.warn("Emergencia {}: Bonita no mandó una respuesta válida del Sistema Nacional. Recibido: {}", id, cuerpo);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Falta la respuesta del Sistema Nacional. Se recibió: " + abreviar(cuerpo));
        }
        return valor;
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
