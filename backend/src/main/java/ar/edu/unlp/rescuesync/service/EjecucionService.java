package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.dto.DespliegueDTO;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.ResultadoCompromisoDTO;
import ar.edu.unlp.rescuesync.entity.Aporte;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.ONG;
import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.ResultadoOferta;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.OfertaRepository;
import ar.edu.unlp.rescuesync.sistemanacional.CompromisoSN;
import ar.edu.unlp.rescuesync.sistemanacional.SistemaNacionalClient;
import ar.edu.unlp.rescuesync.sistemanacional.ValidacionSN;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

// Etapas 6 y 7 del proceso: compromiso de recursos, ejecución, finalización y cierre.
//  1. "Comprometer Recursos" (Bonita): pide el request, lo manda al SN y nos reenvía la respuesta.
//     201 -> sigue a "Monitorear". 409 -> la emergencia vuelve a "Adjudicar y notificar".
//  2. "Monitorear estado de despliegue" (CCR): revisa lo comprometido y da inicio al despliegue.
//  3. "Ejecutar y marcar tareas finalizadas" (ONG): cada ONG marca su parte y el backend le avisa al SN,
//     que libera sus recursos. Cuando termina la última, se completa la tarea en Bonita.
//  4. Al completarse esa tarea, Bonita cierra el proyecto en el SN y nos avisa: la emergencia queda CERRADA.
@Service
public class EjecucionService {

    static final String TAREA_MONITOREAR = "Monitorear estado de despliegue";
    static final String TAREA_EJECUTAR = "Ejecutar y marcar tareas finalizadas";

    private final EmergenciaRepository emergenciaRepository;
    private final OfertaRepository ofertaRepository;
    private final BonitaClient bonitaClient;
    private final SistemaNacionalClient sistemaNacional;

    public EjecucionService(EmergenciaRepository emergenciaRepository, OfertaRepository ofertaRepository,
                            BonitaClient bonitaClient, SistemaNacionalClient sistemaNacional) {
        this.emergenciaRepository = emergenciaRepository;
        this.ofertaRepository = ofertaRepository;
        this.bonitaClient = bonitaClient;
        this.sistemaNacional = sistemaNacional;
    }

    // ========================================================================
    // Bonita: request de compromiso con las ofertas adjudicadas (se manda al SN tal cual)
    // ========================================================================
    @Transactional(readOnly = true)
    public CompromisoSN.Request compromisoParaBonita(Long emergenciaId) {
        Emergencia emergencia = buscar(emergenciaId);
        if (emergencia.getEstado() != EstadoEmergencia.ADJUDICADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La emergencia " + emergenciaId + " no está adjudicada (estado: " + emergencia.getEstado() + ")");
        }

        List<ValidacionSN.OfertaRequest> ofertas = adjudicadas(emergenciaId).stream()
                .map(o -> new ValidacionSN.OfertaRequest(
                        ValidacionSN.ofertaRef(o.getId()),
                        ValidacionSN.loteRef(o.getLote().getId()),
                        o.getLote().getRecursoCodigo(),
                        o.getLote().getCantidad(),
                        asignados(o).stream()
                                .map(a -> new ValidacionSN.AporteRequest(a.getOng().getCuit(), a.getCantidad()))
                                .toList()))
                .toList();

        return new CompromisoSN.Request(
                ValidacionSN.emergenciaRef(emergencia.getId()),
                emergencia.getGravedad().name(),
                texto(emergencia.getTipoDesastre().name()) + " en " + emergencia.getZonaAfectada(),
                ofertas);
    }

    // ========================================================================
    // Bonita: resultado del compromiso en el SN
    // ========================================================================
    @Transactional
    public ResultadoCompromisoDTO registrarCompromiso(Long emergenciaId, CompromisoSN.Respuesta respuesta) {
        Emergencia emergencia = buscar(emergenciaId);
        if (emergencia.getEstado() != EstadoEmergencia.ADJUDICADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La emergencia " + emergenciaId + " no está adjudicada (estado: " + emergencia.getEstado() + ")");
        }

        // 201: quedaron todos los recursos bloqueados
        if (respuesta.compromisos() != null) {
            emergencia.setAvisoCompromiso(null);
            return new ResultadoCompromisoDTO(emergenciaId, true, null);
        }

        // 409: no se bloqueó nada (todo o nada). La oferta que falló ya no se puede elegir
        // y el municipio vuelve a adjudicar.
        List<Oferta> ofertas = ofertaRepository.findByLote_Emergencia_IdAndEstadoOrderByIdAsc(emergenciaId, EstadoOferta.ACTIVA);
        Map<Long, Oferta> porId = ofertas.stream().collect(Collectors.toMap(Oferta::getId, o -> o));

        Map<Oferta, String> rechazos = new LinkedHashMap<>();
        for (CompromisoSN.Item item : Optional.ofNullable(respuesta.items()).orElse(List.<CompromisoSN.Item>of())) {
            Oferta oferta = porId.get(ValidacionSN.idDe(item.ofertaRef(), "OF"));
            if (item.ok() || oferta == null) continue;
            String motivo = nombreOng(oferta, item.cuit()) + ": " + item.motivo();
            rechazos.merge(oferta, motivo, (a, b) -> a + "; " + b);
        }
        rechazos.forEach(Oferta::setMotivoRechazoCompromiso);
        ofertas.forEach(o -> o.setResultado(null));

        String detalle = rechazos.isEmpty()
                ? Optional.ofNullable(respuesta.detail()).orElse("el Sistema Nacional rechazó el compromiso")
                : rechazos.entrySet().stream()
                        .map(e -> e.getKey().getRecurso() + " (" + e.getValue() + ")")
                        .collect(Collectors.joining(". "));
        emergencia.setAvisoCompromiso("No se pudieron comprometer los recursos: " + detalle
                + ". Elegí de nuevo; las ofertas rechazadas ya no aparecen.");
        emergencia.setEstado(EstadoEmergencia.EN_EVALUACION);

        return new ResultadoCompromisoDTO(emergenciaId, false, emergencia.getAvisoCompromiso());
    }

    // ========================================================================
    // Bonita: el SN cerró el proyecto -> la emergencia queda CERRADA
    // ========================================================================
    @Transactional
    public EmergenciaDTO registrarCierre(Long emergenciaId, CompromisoSN.Cierre cierre) {
        Emergencia emergencia = buscar(emergenciaId);
        if (cierre == null || !"CERRADO".equals(cierre.estadoProyecto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El Sistema Nacional no informó el proyecto como cerrado");
        }
        emergencia.setEstado(EstadoEmergencia.CERRADA);
        return EmergenciaDTO.desde(emergencia);
    }

    // ========================================================================
    // Coordinador: seguimiento del despliegue
    // ========================================================================
    @Transactional(readOnly = true)
    public List<DespliegueDTO> despliegues() {
        return emergenciaRepository.findByEstadoInOrderByIdDesc(List.of(
                        EstadoEmergencia.ADJUDICADA, EstadoEmergencia.EN_EJECUCION, EstadoEmergencia.CERRADA))
                .stream()
                .map(this::despliegueDe)
                .toList();
    }

    @Transactional(readOnly = true)
    public DespliegueDTO despliegue(Long emergenciaId) {
        return despliegueDe(buscar(emergenciaId));
    }

    // "Monitorear estado de despliegue": el coordinador revisó lo comprometido y da inicio a la ejecución
    @Transactional
    public DespliegueDTO iniciarDespliegue(Long emergenciaId, UsuarioLogueado coordinador) {
        Emergencia emergencia = buscar(emergenciaId);
        if (emergencia.getEstado() != EstadoEmergencia.ADJUDICADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El despliegue solo se puede iniciar en una emergencia adjudicada (estado: " + emergencia.getEstado() + ")");
        }
        long tareaId = tareaLista(coordinador, emergencia, TAREA_MONITOREAR);
        emergencia.setEstado(EstadoEmergencia.EN_EJECUCION);
        emergenciaRepository.flush();   // si la base rechaza algo, que falle antes de avanzar en Bonita
        bonitaClient.ejecutarTarea(coordinador.bonita(), tareaId, Map.of());
        return despliegueDe(emergencia);
    }

    // ========================================================================
    // ONG: marcar sus actividades como finalizadas
    // Primero se le avisa al SN (libera sus recursos; es idempotente, se puede reintentar).
    // Si es la última ONG, se completa la tarea de Bonita, que después cierra el proyecto.
    // ========================================================================
    @Transactional
    public DespliegueDTO finalizar(Long emergenciaId, UsuarioLogueado ong) {
        Emergencia emergencia = buscar(emergenciaId);
        if (emergencia.getEstado() != EstadoEmergencia.EN_EJECUCION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El despliegue de esta emergencia no está en ejecución (estado: " + emergencia.getEstado() + ")");
        }

        List<Aporte> asignados = adjudicadas(emergenciaId).stream().flatMap(o -> asignados(o).stream()).toList();
        List<Aporte> mios = asignados.stream().filter(a -> a.getOng().getId().equals(ong.organizacionId())).toList();
        if (mios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu ONG no tiene recursos adjudicados en esta emergencia");
        }
        if (mios.stream().allMatch(a -> a.getFechaFinalizacion() != null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya marcaste tus actividades como finalizadas");
        }

        ONG organizacion = mios.get(0).getOng();
        sistemaNacional.finalizarOng(ValidacionSN.emergenciaRef(emergenciaId), organizacion.getCuit());
        mios.stream().filter(a -> a.getFechaFinalizacion() == null).forEach(Aporte::finalizar);

        if (asignados.stream().allMatch(a -> a.getFechaFinalizacion() != null)) {
            long tareaId = tareaLista(ong, emergencia, TAREA_EJECUTAR);
            emergenciaRepository.flush();   // si la base rechaza algo, que falle antes de avanzar en Bonita
            bonitaClient.ejecutarTarea(ong.bonita(), tareaId, Map.of());
        }
        return despliegueDe(emergencia);
    }

    // ---- helpers -----------------------------------------------------------------

    private DespliegueDTO despliegueDe(Emergencia emergencia) {
        List<DespliegueDTO.Asignacion> asignaciones = adjudicadas(emergencia.getId()).stream()
                .map(o -> new DespliegueDTO.Asignacion(o.getId(), o.getLote().getId(), o.getRecurso(),
                        o.getLote().getCantidad(), o.esConsorcio(),
                        asignados(o).stream()
                                .map(a -> new DespliegueDTO.Participante(a.getOng().getId(), a.getOng().getNombre(),
                                        a.getCantidad(),
                                        a.getRolValidacion() == null ? null : a.getRolValidacion().name(),
                                        a.getFechaFinalizacion()))
                                .toList()))
                .toList();

        // Una ONG terminó cuando terminó en todas las ofertas en las que participa
        Map<Long, Boolean> ongs = new LinkedHashMap<>();
        asignaciones.forEach(asig -> asig.ongs().forEach(p ->
                ongs.merge(p.ongId(), p.fechaFinalizacion() != null, Boolean::logicalAnd)));

        return new DespliegueDTO(EmergenciaDTO.desde(emergencia), asignaciones, ongs.size(),
                (int) ongs.values().stream().filter(b -> b).count());
    }

    private List<Oferta> adjudicadas(Long emergenciaId) {
        return ofertaRepository.findByLote_Emergencia_IdAndEstadoOrderByIdAsc(emergenciaId, EstadoOferta.ACTIVA)
                .stream()
                .filter(o -> o.getResultado() == ResultadoOferta.ADJUDICADA)
                .toList();
    }

    // Las ONGs que efectivamente ponen recursos en la oferta
    private static List<Aporte> asignados(Oferta oferta) {
        return oferta.getAportes().stream()
                .filter(a -> a.getEstado() == EstadoAporte.CONFIRMADO && a.getCantidad() > 0)
                .toList();
    }

    private static String nombreOng(Oferta oferta, String cuit) {
        return oferta.getAportes().stream()
                .filter(a -> Objects.equals(a.getOng().getCuit(), cuit))
                .map(a -> a.getOng().getNombre())
                .findFirst()
                .orElse("CUIT " + cuit);
    }

    // La tarea de Bonita con ese nombre, lista para este usuario, en el case de la emergencia
    private long tareaLista(UsuarioLogueado usuario, Emergencia emergencia, String nombre) {
        return bonitaClient.tareaLista(usuario.bonita(), emergencia.getBonitaCaseId(), nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bonita todavía no tiene lista la tarea \"" + nombre + "\" de esta emergencia. Probá de nuevo en unos segundos."));
    }

    private Emergencia buscar(Long emergenciaId) {
        return emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la emergencia " + emergenciaId));
    }

    // INUNDACION -> "Inundacion" (para la descripción del proyecto en el SN)
    private static String texto(String valor) {
        String minusculas = valor.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(minusculas.charAt(0)) + minusculas.substring(1);
    }
}
