package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.*;
import ar.edu.unlp.rescuesync.entity.*;
import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// Todo lo de las ofertas de las ONGs. Vive SOLO en Postgres (directiva 1 del enunciado):
// Bonita no se entera de cada oferta; cuando vence el timer, pide el consolidado (cobertura).
@Service
public class OfertaService {

    // Participaciones "vivas": las que cuentan como que la ONG está en la oferta
    private static final List<EstadoAporte> VIVOS = List.of(EstadoAporte.PENDIENTE, EstadoAporte.CONFIRMADO);

    private final OfertaRepository ofertaRepository;
    private final AporteRepository aporteRepository;
    private final OfertaVersionRepository versionRepository;
    private final ONGRepository ongRepository;
    private final LoteRepository loteRepository;
    private final EmergenciaRepository emergenciaRepository;

    public OfertaService(OfertaRepository ofertaRepository,
                         AporteRepository aporteRepository,
                         OfertaVersionRepository versionRepository,
                         ONGRepository ongRepository,
                         LoteRepository loteRepository,
                         EmergenciaRepository emergenciaRepository) {
        this.ofertaRepository = ofertaRepository;
        this.aporteRepository = aporteRepository;
        this.versionRepository = versionRepository;
        this.ongRepository = ongRepository;
        this.loteRepository = loteRepository;
        this.emergenciaRepository = emergenciaRepository;
    }

    // ========================================================================
    // ONG: crear una oferta (individual o invitando a otras = consorcio)
    // ========================================================================
    @Transactional
    public OfertaDTO crearOferta(NuevaOfertaDTO dto, UsuarioLogueado usuario) {

        ONG ong = ongDe(usuario);

        if (dto.getLoteId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Falta el lote");
        }
        Lote lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe el lote " + dto.getLoteId()));

        validarVentanaAbierta(lote.getEmergencia());
        validarCantidad(dto.getCantidad(), lote.getCantidad());

        // Una ONG participa en UNA sola oferta por lote: si ya está, que edite esa
        if (participaEnLote(ong.getId(), lote.getId())) {
            throw error(HttpStatus.CONFLICT,
                    "Ya participás en una oferta para este lote. Modificala en lugar de crear otra.");
        }

        // La oferta: la ONG que la crea queda como líder con su aporte confirmado
        Oferta oferta = new Oferta(ong, lote);
        oferta.agregarAporte(new Aporte(oferta, ong, dto.getCantidad(), EstadoAporte.CONFIRMADO));

        // Consorcio: las ONGs invitadas entran como PENDIENTES (todavía no suman)
        List<String> invitadas = new ArrayList<>();
        for (Long invitadaId : Optional.ofNullable(dto.getOngsInvitadas()).orElse(List.of())) {
            if (invitadaId == null || invitadaId.equals(ong.getId())) continue;
            ONG invitada = ongRepository.findById(invitadaId)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe la ONG " + invitadaId));
            if (participaEnLote(invitadaId, lote.getId())) {
                throw error(HttpStatus.CONFLICT,
                        invitada.getNombre() + " ya participa en otra oferta para este lote");
            }
            if (oferta.aporteDe(invitadaId).isEmpty()) {
                oferta.agregarAporte(new Aporte(oferta, invitada, 0, EstadoAporte.PENDIENTE));
                invitadas.add(invitada.getNombre());
            }
        }

        oferta.recalcularTotal();
        String accion = invitadas.isEmpty()
                ? "Creó la oferta con " + dto.getCantidad()
                : "Creó la oferta con " + dto.getCantidad() + " e invitó a " + String.join(", ", invitadas);
        // Primero se guarda la oferta (con sus aportes, por el cascade) y después su versión 1
        Oferta guardada = ofertaRepository.save(oferta);
        registrarVersion(guardada, ong, accion);

        return OfertaDTO.desde(guardada, ong.getId());
    }

    // ========================================================================
    // ONG: cambiar su aporte. Si era una invitación pendiente, esto la ACEPTA.
    // ========================================================================
    @Transactional
    public OfertaDTO actualizarAporte(Long ofertaId, int cantidad, UsuarioLogueado usuario) {

        ONG ong = ongDe(usuario);
        Oferta oferta = ofertaActiva(ofertaId);
        validarVentanaAbierta(oferta.getLote().getEmergencia());

        Aporte aporte = oferta.aporteDe(ong.getId())
                .filter(a -> VIVOS.contains(a.getEstado()))
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "No participás en esta oferta"));

        int requerido = oferta.getLote().getCantidad();
        validarCantidad(cantidad, requerido);

        // El total de la oferta (sumando a las demás ONGs) no puede pasar lo que pide el lote
        int deLasDemas = oferta.getAportes().stream()
                .filter(a -> a != aporte && a.getEstado() == EstadoAporte.CONFIRMADO)
                .mapToInt(Aporte::getCantidad).sum();
        if (deLasDemas + cantidad > requerido) {
            throw error(HttpStatus.BAD_REQUEST, "Con ese aporte la oferta sumaría " + (deLasDemas + cantidad)
                    + " y el lote pide " + requerido + ". Podés aportar hasta " + (requerido - deLasDemas) + ".");
        }

        String accion = aporte.getEstado() == EstadoAporte.PENDIENTE
                ? "Aceptó sumarse al consorcio con " + cantidad
                : "Modificó su aporte de " + aporte.getCantidad() + " a " + cantidad;

        aporte.actualizar(cantidad, EstadoAporte.CONFIRMADO);
        oferta.recalcularTotal();
        registrarVersion(oferta, ong, accion);

        return OfertaDTO.desde(oferta, ong.getId());
    }

    // ========================================================================
    // ONG: retirarse.
    //  - Si es la líder: se retira la oferta entera (y el consorcio).
    //  - Si es invitada pendiente: rechaza la invitación.
    //  - Si es miembro confirmado: sale del consorcio.
    // ========================================================================
    @Transactional
    public OfertaDTO retirar(Long ofertaId, UsuarioLogueado usuario) {

        ONG ong = ongDe(usuario);
        Oferta oferta = ofertaActiva(ofertaId);
        validarVentanaAbierta(oferta.getLote().getEmergencia());

        Aporte aporte = oferta.aporteDe(ong.getId())
                .filter(a -> VIVOS.contains(a.getEstado()))
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "No participás en esta oferta"));

        String accion;
        if (oferta.esLider(ong.getId())) {
            oferta.setEstado(EstadoOferta.RETIRADA);
            accion = "Retiró la oferta";
        } else if (aporte.getEstado() == EstadoAporte.PENDIENTE) {
            aporte.actualizar(0, EstadoAporte.RECHAZADO);
            accion = "Rechazó la invitación al consorcio";
        } else {
            aporte.actualizar(aporte.getCantidad(), EstadoAporte.RETIRADO);
            accion = "Se retiró del consorcio";
        }

        oferta.recalcularTotal();
        registrarVersion(oferta, ong, accion);

        return OfertaDTO.desde(oferta, ong.getId());
    }

    // ========================================================================
    // ONG: sus ofertas activas e invitaciones pendientes
    // ========================================================================
    @Transactional(readOnly = true)
    public List<OfertaDTO> misOfertas(UsuarioLogueado usuario) {
        ONG ong = ongDe(usuario);
        return aporteRepository.findByOng_IdAndOferta_EstadoAndEstadoIn(ong.getId(), EstadoOferta.ACTIVA, VIVOS)
                .stream()
                .map(Aporte::getOferta)
                .distinct()
                .map(o -> OfertaDTO.desde(o, ong.getId()))
                .toList();
    }

    // ========================================================================
    // Historial (trazabilidad). Lo ve quien participa en la oferta y el coordinador.
    // ========================================================================
    @Transactional(readOnly = true)
    public List<OfertaVersionDTO> historial(Long ofertaId, UsuarioLogueado usuario, boolean esCoordinador) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe la oferta " + ofertaId));

        if (!esCoordinador && oferta.aporteDe(usuario.organizacionId()).isEmpty()) {
            throw error(HttpStatus.FORBIDDEN, "No participás en esta oferta");
        }

        return versionRepository.findByOferta_IdOrderByNumeroDesc(ofertaId)
                .stream().map(OfertaVersionDTO::desde).toList();
    }

    // ONGs para invitar a un consorcio (todas menos la propia)
    @Transactional(readOnly = true)
    public List<OngDTO> ongsParaInvitar(UsuarioLogueado usuario) {
        return ongRepository.findByIdNotOrderByNombreAsc(ongDe(usuario).getId())
                .stream().map(o -> new OngDTO(o.getId(), o.getNombre())).toList();
    }

    // ========================================================================
    // Cobertura: cuánto se ofreció por lote. La usan el coordinador y Bonita.
    // Las ONGs pueden ofertar aunque un lote ya esté cubierto: la elección se hace después del cierre.
    // ========================================================================
    @Transactional(readOnly = true)
    public CoberturaDTO cobertura(Long emergenciaId) {

        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe la emergencia " + emergenciaId));

        List<Lote> lotes = loteRepository.findByEmergencia_IdOrderByIdAsc(emergenciaId);

        // Ofertas activas agrupadas por lote
        Map<Long, List<Oferta>> porLote = ofertaRepository
                .findByLote_Emergencia_IdAndEstadoOrderByIdAsc(emergenciaId, EstadoOferta.ACTIVA)
                .stream()
                .collect(Collectors.groupingBy(o -> o.getLote().getId()));

        List<CoberturaLoteDTO> detalle = new ArrayList<>();
        int totalOfertas = 0;

        for (Lote lote : lotes) {
            List<Oferta> ofertas = porLote.getOrDefault(lote.getId(), List.of());
            totalOfertas += ofertas.size();

            int requerido = lote.getCantidad();
            int ofrecido = ofertas.stream().mapToInt(Oferta::getCantidad).sum();
            int faltante = Math.max(0, requerido - ofrecido);
            int porcentaje = requerido == 0 ? 100 : Math.min(100, ofrecido * 100 / requerido);

            detalle.add(new CoberturaLoteDTO(
                    lote.getId(), lote.getRecurso(), requerido, ofrecido, faltante, porcentaje,
                    ofrecido >= requerido,
                    ofertas.stream().map(o -> OfertaDTO.desde(o, null)).toList()
            ));
        }

        // Todos los lotes cubiertos -> la compuerta de Bonita va por "Sí cubre"
        boolean lotesCubiertos = !detalle.isEmpty() && detalle.stream().allMatch(CoberturaLoteDTO::cubierto);

        return new CoberturaDTO(EmergenciaDTO.desde(emergencia), lotesCubiertos, totalOfertas, detalle);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    // La ONG sale SIEMPRE de la sesión: nadie opera en nombre de otra
    private ONG ongDe(UsuarioLogueado usuario) {
        if (usuario.organizacionId() == null) {
            throw error(HttpStatus.FORBIDDEN, "El usuario no pertenece a una ONG");
        }
        return ongRepository.findById(usuario.organizacionId())
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "El usuario no pertenece a una ONG"));
    }

    private Oferta ofertaActiva(Long ofertaId) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe la oferta " + ofertaId));
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw error(HttpStatus.CONFLICT, "La oferta fue retirada");
        }
        return oferta;
    }

    // Solo se puede ofertar/modificar con la convocatoria abierta y antes del cierre
    private void validarVentanaAbierta(Emergencia emergencia) {
        if (emergencia.getEstado() != EstadoEmergencia.EN_CONVOCATORIA) {
            throw error(HttpStatus.CONFLICT, "La convocatoria no está abierta");
        }
        if (emergencia.getFechaCierreOfertas() != null
                && LocalDateTime.now().isAfter(emergencia.getFechaCierreOfertas())) {
            throw error(HttpStatus.CONFLICT, "La ventana de ofertas ya cerró");
        }
    }

    private void validarCantidad(int cantidad, int requerido) {
        if (cantidad < 1 || cantidad > requerido) {
            throw error(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y " + requerido);
        }
    }

    private boolean participaEnLote(Long ongId, Long loteId) {
        return aporteRepository.existsByOng_IdAndOferta_Lote_IdAndOferta_EstadoAndEstadoIn(
                ongId, loteId, EstadoOferta.ACTIVA, VIVOS);
    }

    // Guarda una versión nueva en el historial con la "foto" de los aportes
    private void registrarVersion(Oferta oferta, ONG autor, String accion) {
        int numero = oferta.nuevaVersion();
        String detalle = oferta.getAportes().stream()
                .map(a -> a.getOng().getNombre() + ": " + a.getCantidad()
                        + (a.getEstado() == EstadoAporte.CONFIRMADO ? "" : " (" + a.getEstado().name().toLowerCase() + ")"))
                .collect(Collectors.joining(" · "));
        versionRepository.save(new OfertaVersion(oferta, numero, autor.getNombre(), accion, oferta.getCantidad(), detalle));
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
