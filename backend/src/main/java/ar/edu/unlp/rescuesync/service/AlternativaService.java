package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.dto.CoberturaLoteDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.NuevoLoteDTO;
import ar.edu.unlp.rescuesync.dto.PublicarLotesDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import ar.edu.unlp.rescuesync.repository.OfertaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Camino alternativo: venció el plazo y no todos los lotes tienen una oferta validada que los cubra.
// "Evaluar curso de acción alternativo" (coordinador) decide entre:
//  - REABRIR: convocatoria desde cero. Puede bajar cantidades o sacar recursos; las ofertas anteriores se descartan.
//  - PARCIAL: seguir con los lotes que sí tienen oferta validada; el municipio adjudica solo esos.
// La decisión viaja en el contrato de la tarea y una compuerta de Bonita la usa para elegir el camino.
@Service
public class AlternativaService {

    static final String TAREA = "Evaluar curso de acción alternativo";

    private final EmergenciaRepository emergenciaRepository;
    private final LoteRepository loteRepository;
    private final OfertaRepository ofertaRepository;
    private final OfertaService ofertaService;
    private final BonitaClient bonitaClient;

    public AlternativaService(EmergenciaRepository emergenciaRepository, LoteRepository loteRepository,
                              OfertaRepository ofertaRepository, OfertaService ofertaService,
                              BonitaClient bonitaClient) {
        this.emergenciaRepository = emergenciaRepository;
        this.loteRepository = loteRepository;
        this.ofertaRepository = ofertaRepository;
        this.ofertaService = ofertaService;
        this.bonitaClient = bonitaClient;
    }

    // ========================================================================
    // REABRIR: lotes nuevos (mismos recursos o menos, otras cantidades) y un plazo nuevo
    // ========================================================================
    @Transactional
    public EmergenciaDTO reabrir(Long emergenciaId, PublicarLotesDTO dto, UsuarioLogueado coordinador) {
        Emergencia emergencia = enEvaluacion(emergenciaId);
        long tareaId = tarea(coordinador, emergencia);

        if (dto.getDuracionMinutos() == null || dto.getDuracionMinutos() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La duración debe ser de al menos 1 minuto");
        }
        if (dto.getLotes() == null || dto.getLotes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay que dejar al menos un lote");
        }

        // Solo se pueden mantener (o achicar) recursos que ya estaban: no se agregan nuevos
        List<Lote> anteriores = loteRepository.vigentesDe(emergenciaId);
        Map<String, Lote> porCodigo = anteriores.stream()
                .collect(Collectors.toMap(Lote::getRecursoCodigo, Function.identity(), (a, b) -> a));
        Set<String> usados = new HashSet<>();
        for (NuevoLoteDTO nuevo : dto.getLotes()) {
            if (nuevo.getRecursoCodigo() == null || !porCodigo.containsKey(nuevo.getRecursoCodigo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Al reabrir solo se pueden mantener o sacar recursos de la convocatoria anterior");
            }
            if (!usados.add(nuevo.getRecursoCodigo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada recurso puede estar en un solo lote");
            }
            if (nuevo.getCantidad() < 1 || nuevo.getCantidad() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada lote necesita una cantidad entre 1 y 100");
            }
        }

        // Desde cero: las ofertas anteriores se descartan y los lotes viejos quedan archivados
        ofertaRepository.findByLote_Emergencia_IdAndEstadoOrderByIdAsc(emergenciaId, EstadoOferta.ACTIVA)
                .forEach(o -> ofertaService.descartar(o, coordinador.nombre()));
        anteriores.forEach(Lote::archivar);
        for (NuevoLoteDTO nuevo : dto.getLotes()) {
            Lote anterior = porCodigo.get(nuevo.getRecursoCodigo());
            Lote lote = new Lote(anterior.getRecurso(), anterior.getRecursoCodigo(), nuevo.getCantidad(), emergencia);
            emergencia.addLote(lote);
            loteRepository.save(lote);
        }

        emergencia.setFechaCierreOfertas(LocalDateTime.now().plusMinutes(dto.getDuracionMinutos()));
        emergencia.setEstado(EstadoEmergencia.EN_CONVOCATORIA);
        emergencia.setAvisoCompromiso(null);
        emergencia.setCoberturaParcial(false);

        // Bonita: la compuerta va a "Publicar Convocatoria" y el timer arranca con la duración nueva
        emergenciaRepository.flush();   // si la base rechaza algo, que falle antes de avanzar en Bonita
        bonitaClient.ejecutarTarea(coordinador.bonita(), tareaId, Map.of(
                "decisionInput", "REABRIR",
                "duracionVentanaMsInput", dto.getDuracionMinutos() * 60_000L));
        return EmergenciaDTO.desde(emergencia);
    }

    // ========================================================================
    // PARCIAL: el municipio adjudica los lotes que tienen oferta validada; el resto queda sin cubrir
    // ========================================================================
    @Transactional
    public EmergenciaDTO continuarConCoberturaParcial(Long emergenciaId, UsuarioLogueado coordinador) {
        Emergencia emergencia = enEvaluacion(emergenciaId);
        long tareaId = tarea(coordinador, emergencia);

        boolean algunoCubierto = ofertaService.cobertura(emergenciaId).lotes().stream()
                .anyMatch(CoberturaLoteDTO::habilitado);
        if (!algunoCubierto) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ningún lote tiene una oferta validada por el Sistema Nacional: no hay nada para adjudicar. Reabrí la convocatoria.");
        }

        emergencia.setCoberturaParcial(true);

        // Bonita: la compuerta va a "Adjudicar y notificar" (la duración no se usa en este camino)
        emergenciaRepository.flush();   // si la base rechaza algo, que falle antes de avanzar en Bonita
        bonitaClient.ejecutarTarea(coordinador.bonita(), tareaId, Map.of(
                "decisionInput", "PARCIAL",
                "duracionVentanaMsInput", 0));
        return EmergenciaDTO.desde(emergencia);
    }

    // ---- helpers -----------------------------------------------------------------

    private Emergencia enEvaluacion(Long emergenciaId) {
        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la emergencia " + emergenciaId));
        if (emergencia.getEstado() != EstadoEmergencia.EN_EVALUACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La convocatoria no está esperando una decisión (estado: " + emergencia.getEstado() + ")");
        }
        return emergencia;
    }

    private long tarea(UsuarioLogueado coordinador, Emergencia emergencia) {
        return bonitaClient.tareaLista(coordinador.bonita(), emergencia.getBonitaCaseId(), TAREA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bonita no tiene lista la tarea \"" + TAREA + "\" de esta emergencia"));
    }
}
