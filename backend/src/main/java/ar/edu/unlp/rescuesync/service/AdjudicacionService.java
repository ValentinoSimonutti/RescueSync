package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import ar.edu.unlp.rescuesync.dto.AdjudicacionDTO;
import ar.edu.unlp.rescuesync.dto.AdjudicarDTO;
import ar.edu.unlp.rescuesync.dto.ConsolidadoBonitaDTO;
import ar.edu.unlp.rescuesync.dto.CoberturaDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.OfertaDTO;
import ar.edu.unlp.rescuesync.dto.ResultadoValidacionDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.ResultadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.RolValidacion;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import ar.edu.unlp.rescuesync.repository.OfertaRepository;
import ar.edu.unlp.rescuesync.sistemanacional.ValidacionSN;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

// Etapas 4 y 5 del proceso: validación externa (Sistema Nacional) y adjudicación del municipio.
//  1. Vence el timer -> Bonita pide el consolidado, que trae armado el request de validación.
//  2. Bonita llama al Sistema Nacional y nos reenvía la respuesta: se guarda el perfil de cada oferta.
//  3. El municipio ve solo las ofertas validadas que cubren su lote y elige una por lote.
@Service
public class AdjudicacionService {

    private final EmergenciaRepository emergenciaRepository;
    private final LoteRepository loteRepository;
    private final OfertaRepository ofertaRepository;
    private final OfertaService ofertaService;
    private final BonitaClient bonitaClient;

    public AdjudicacionService(EmergenciaRepository emergenciaRepository, LoteRepository loteRepository,
                               OfertaRepository ofertaRepository, OfertaService ofertaService,
                               BonitaClient bonitaClient) {
        this.emergenciaRepository = emergenciaRepository;
        this.loteRepository = loteRepository;
        this.ofertaRepository = ofertaRepository;
        this.ofertaService = ofertaService;
        this.bonitaClient = bonitaClient;
    }

    // ========================================================================
    // Bonita: consolidado de ofertas + request listo para el Sistema Nacional
    // ========================================================================
    @Transactional(readOnly = true)
    public ConsolidadoBonitaDTO consolidadoParaBonita(Long emergenciaId) {
        CoberturaDTO cobertura = ofertaService.cobertura(emergenciaId);
        Emergencia emergencia = buscar(emergenciaId);

        // Solo las ofertas que cubren su lote: las incompletas (consorcio sin terminar) no se pueden adjudicar
        List<ValidacionSN.OfertaRequest> ofertas = ofertasActivas(emergenciaId).stream()
                .filter(o -> o.getCantidad() >= o.getLote().getCantidad())
                .map(o -> new ValidacionSN.OfertaRequest(
                        ValidacionSN.ofertaRef(o.getId()),
                        ValidacionSN.loteRef(o.getLote().getId()),
                        o.getLote().getRecursoCodigo(),
                        o.getLote().getCantidad(),
                        o.getAportes().stream()
                                .filter(a -> a.getEstado() == EstadoAporte.CONFIRMADO && a.getCantidad() > 0)
                                .map(a -> new ValidacionSN.AporteRequest(a.getOng().getCuit(), a.getCantidad()))
                                .toList()))
                .toList();

        ValidacionSN.Request request = new ValidacionSN.Request(
                ValidacionSN.emergenciaRef(emergencia.getId()),
                emergencia.getGravedad().name(),
                emergencia.getTipoDesastre().name(),
                ofertas);

        return new ConsolidadoBonitaDTO(emergenciaId, cobertura.lotesCubiertos(), cobertura.totalOfertas(),
                cobertura.lotes(), request);
    }

    // ========================================================================
    // Bonita: guardar la respuesta del Sistema Nacional (idempotente: se puede reenviar)
    // ========================================================================
    @Transactional
    public ResultadoValidacionDTO registrarValidacion(Long emergenciaId, ValidacionSN.Response respuesta) {
        Emergencia emergencia = buscar(emergenciaId);
        if (respuesta == null || respuesta.ofertas() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la respuesta del Sistema Nacional");
        }
        if (respuesta.emergenciaRef() != null
                && !respuesta.emergenciaRef().equals(ValidacionSN.emergenciaRef(emergencia.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La validación es de " + respuesta.emergenciaRef() + ", no de la emergencia " + emergenciaId);
        }

        Map<Long, Oferta> ofertas = ofertasActivas(emergenciaId).stream()
                .collect(Collectors.toMap(Oferta::getId, o -> o));

        // Lo que no vuelve validado (ej. ofertas incompletas que no se mandaron) queda sin habilitación
        ofertas.values().forEach(o -> {
            o.registrarValidacion(RolValidacion.SIN_HABILITACION_VIGENTE, 0, "No fue validada por el Sistema Nacional.");
            o.getAportes().forEach(a -> a.registrarValidacion(null, 0));
        });

        int validadas = 0;
        for (ValidacionSN.OfertaValidada validada : respuesta.ofertas()) {
            Oferta oferta = ofertas.get(ValidacionSN.idDe(validada.ofertaRef(), "OF"));
            if (oferta == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La oferta " + validada.ofertaRef() + " no es una oferta activa de la emergencia " + emergenciaId);
            }
            oferta.registrarValidacion(rol(validada.rolOferta()), validada.cantidadValidada(),
                    String.join("\n", observaciones(validada)));

            // Perfil de cada ONG del aporte (se cruza por CUIT)
            for (ValidacionSN.Perfil perfil : Optional.ofNullable(validada.aportes()).orElse(List.<ValidacionSN.Perfil>of())) {
                oferta.getAportes().stream()
                        .filter(a -> Objects.equals(a.getOng().getCuit(), perfil.cuit()))
                        .findFirst()
                        .ifPresent(a -> a.registrarValidacion(rol(perfil.rolAsignado()), perfil.cantidadValidable()));
            }
            validadas++;
        }

        int adjudicables = (int) ofertas.values().stream().filter(Oferta::esAdjudicable).count();
        return new ResultadoValidacionDTO(emergenciaId, todosLosLotesTienenOpcion(emergenciaId, ofertas.values()),
                validadas, adjudicables);
    }

    // ========================================================================
    // Municipio: qué puede elegir en "Adjudicar y notificar"
    // ========================================================================
    @Transactional(readOnly = true)
    public AdjudicacionDTO opciones(Long emergenciaId, UsuarioLogueado usuario) {
        Emergencia emergencia = delMunicipio(emergenciaId, usuario);
        List<Oferta> ofertas = ofertasActivas(emergenciaId);

        List<AdjudicacionDTO.LoteAdjudicable> lotes = loteRepository.vigentesDe(emergenciaId)
                .stream()
                .map(lote -> new AdjudicacionDTO.LoteAdjudicable(
                        lote.getId(), lote.getRecurso(), lote.getRecursoCodigo(), lote.getCantidad(),
                        ofertas.stream()
                                .filter(o -> o.getLote().getId().equals(lote.getId()) && o.esAdjudicable())
                                .map(o -> OfertaDTO.desde(o, null))
                                .toList()))
                .toList();

        return new AdjudicacionDTO(EmergenciaDTO.desde(emergencia), lotes);
    }

    // ========================================================================
    // Municipio: adjudicar (una oferta por lote) y completar la tarea en Bonita
    // @Transactional: si Bonita falla, no queda nada adjudicado.
    // ========================================================================
    @Transactional
    public EmergenciaDTO adjudicar(Long emergenciaId, AdjudicarDTO dto, UsuarioLogueado usuario) {
        Emergencia emergencia = delMunicipio(emergenciaId, usuario);
        if (emergencia.getEstado() != EstadoEmergencia.EN_EVALUACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La emergencia no está en evaluación (estado: " + emergencia.getEstado() + ")");
        }

        // loteId -> ofertaId elegida (no se puede elegir dos veces el mismo lote)
        Map<Long, Long> elegidas = new HashMap<>();
        for (AdjudicarDTO.Seleccion s : Optional.ofNullable(dto.selecciones()).orElse(List.of())) {
            if (s.loteId() == null || s.ofertaId() == null || elegidas.put(s.loteId(), s.ofertaId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí exactamente una oferta por lote");
            }
        }

        List<Oferta> ofertas = ofertasActivas(emergenciaId);
        List<Lote> lotes = loteRepository.vigentesDe(emergenciaId);
        int adjudicados = 0;
        for (Lote lote : lotes) {
            Long ofertaId = elegidas.remove(lote.getId());
            boolean tieneOpciones = ofertas.stream()
                    .anyMatch(o -> o.getLote().getId().equals(lote.getId()) && o.esAdjudicable());
            // Cobertura parcial (lo decidió el coordinador): un lote sin opciones queda sin cubrir
            if (ofertaId == null && emergencia.isCoberturaParcial() && !tieneOpciones) {
                ofertas.stream()
                        .filter(o -> o.getLote().getId().equals(lote.getId()))
                        .forEach(o -> o.setResultado(ResultadoOferta.NO_SELECCIONADA));
                continue;
            }
            if (ofertaId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Falta elegir la oferta para " + lote.getRecurso());
            }
            adjudicados++;
            boolean valida = ofertas.stream().anyMatch(o -> o.getId().equals(ofertaId)
                    && o.getLote().getId().equals(lote.getId()) && o.esAdjudicable());
            if (!valida) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La oferta " + ofertaId + " no es una opción validada para " + lote.getRecurso());
            }
            ofertas.stream()
                    .filter(o -> o.getLote().getId().equals(lote.getId()))
                    .forEach(o -> o.setResultado(o.getId().equals(ofertaId)
                            ? ResultadoOferta.ADJUDICADA : ResultadoOferta.NO_SELECCIONADA));
        }
        if (!elegidas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay lotes elegidos que no son de esta emergencia");
        }
        if (adjudicados == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí al menos una oferta");
        }

        emergencia.setEstado(EstadoEmergencia.ADJUDICADA);

        // Completar "Adjudicar y notificar" con la sesión del municipio -> Bonita sigue a "Comprometer Recursos"
        BonitaSession sesion = usuario.bonita();
        long taskId = bonitaClient.buscarTareaLista(sesion, emergencia.getBonitaCaseId());
        emergenciaRepository.flush();   // si la base rechaza algo, que falle antes de avanzar en Bonita
        bonitaClient.ejecutarTarea(sesion, taskId, Map.of());

        return EmergenciaDTO.desde(emergencia);
    }

    // ---- helpers -----------------------------------------------------------------

    private boolean todosLosLotesTienenOpcion(Long emergenciaId, Collection<Oferta> ofertas) {
        List<Lote> lotes = loteRepository.vigentesDe(emergenciaId);
        return !lotes.isEmpty() && lotes.stream().allMatch(lote -> ofertas.stream()
                .anyMatch(o -> o.getLote().getId().equals(lote.getId()) && o.esAdjudicable()));
    }

    private List<Oferta> ofertasActivas(Long emergenciaId) {
        return ofertaRepository.findByLote_Emergencia_IdAndEstadoOrderByIdAsc(emergenciaId, EstadoOferta.ACTIVA);
    }

    private Emergencia buscar(Long emergenciaId) {
        return emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la emergencia " + emergenciaId));
    }

    // Un municipio solo adjudica sus propias emergencias
    private Emergencia delMunicipio(Long emergenciaId, UsuarioLogueado usuario) {
        Emergencia emergencia = buscar(emergenciaId);
        if (emergencia.getMunicipio() == null || !emergencia.getMunicipio().getId().equals(usuario.organizacionId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La emergencia no es de tu municipio");
        }
        return emergencia;
    }

    // Observaciones de la oferta + las de cada ONG del aporte (ahí viene el motivo de un rechazo,
    // ej. "ONG 1: No tiene la certificación COMBATE_INCENDIOS que requiere BOMBERO_FORESTAL")
    private static List<String> observaciones(ValidacionSN.OfertaValidada validada) {
        List<String> todas = new java.util.ArrayList<>(Optional.ofNullable(validada.observaciones()).orElse(List.of()));
        for (ValidacionSN.Perfil perfil : Optional.ofNullable(validada.aportes()).orElse(List.<ValidacionSN.Perfil>of())) {
            String quien = perfil.nombre() != null ? perfil.nombre() : "CUIT " + perfil.cuit();
            Optional.ofNullable(perfil.observaciones()).orElse(List.of())
                    .forEach(o -> todas.add(quien + ": " + o));
        }
        return todas;
    }

    // El SN manda el rol como texto; uno desconocido se toma como sin habilitación
    private static RolValidacion rol(String valor) {
        try {
            return valor == null ? RolValidacion.SIN_HABILITACION_VIGENTE : RolValidacion.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return RolValidacion.SIN_HABILITACION_VIGENTE;
        }
    }
}
