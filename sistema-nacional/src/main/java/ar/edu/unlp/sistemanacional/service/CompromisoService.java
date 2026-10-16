package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.*;
import ar.edu.unlp.sistemanacional.entity.*;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoCompromiso;
import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;
import ar.edu.unlp.sistemanacional.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

// Compromiso (bloqueo) y liberación de recursos a nivel nacional.
//
// Control de concurrencia (para que dos emergencias en paralelo no se lleven los mismos recursos):
//  1. Se bloquea la fila del Proyecto (FOR UPDATE): las operaciones sobre una misma emergencia van de a una.
//  2. Se averiguan SOLO los ids de las filas de inventario involucradas (sin cargarlas).
//  3. Se bloquean esas filas con SELECT ... FOR UPDATE, en orden de id (sin deadlocks).
//     Otra transacción que quiera las mismas filas espera hasta que esta haga commit,
//     y recién ahí lee la cantidad comprometida actualizada.
//  4. Con las filas bloqueadas se chequea disponibilidad y habilitación, y se descuenta.
//     Si algún aporte no cumple, se lanza la excepción y se hace rollback de todo.
// Leer las filas antes de bloquearlas (paso 2) traería valores viejos: por eso el orden.
@Service
public class CompromisoService {

    private final ProyectoRepository proyectoRepository;
    private final CompromisoRepository compromisoRepository;
    private final RecursoInventarioRepository inventarioRepository;
    private final OngRepository ongRepository;
    private final TipoRecursoRepository tipoRecursoRepository;
    private final EvaluadorCompetencia evaluador;

    public CompromisoService(ProyectoRepository proyectoRepository, CompromisoRepository compromisoRepository,
                             RecursoInventarioRepository inventarioRepository, OngRepository ongRepository,
                             TipoRecursoRepository tipoRecursoRepository, EvaluadorCompetencia evaluador) {
        this.proyectoRepository = proyectoRepository;
        this.compromisoRepository = compromisoRepository;
        this.inventarioRepository = inventarioRepository;
        this.ongRepository = ongRepository;
        this.tipoRecursoRepository = tipoRecursoRepository;
        this.evaluador = evaluador;
    }

    // Un aporte del request, ya normalizado
    private record Item(String ofertaRef, String cuit, boolean cuitValido, String tipoRecurso, int cantidad) {
    }

    // Aporte que pasó todas las restricciones y se va a comprometer
    private record Aprobado(Item item, RecursoInventario inventario, RolAsignado rol) {
    }

    private record Evaluacion(List<ItemVerificacion> items, List<Aprobado> aprobados, List<Compromiso> existentes) {
        boolean factible() {
            return items.stream().allMatch(ItemVerificacion::ok);
        }
    }

    // ---------- Verificación (dry-run, sin bloquear ni escribir) ----------

    @Transactional(readOnly = true)
    public VerificacionResponse verificar(CompromisoRequest request) {
        Proyecto proyecto = proyectoRepository.findByEmergenciaRef(request.emergenciaRef()).orElse(null);
        if (proyecto != null && proyecto.estaCerrado()) {
            throw proyectoCerrado(proyecto);
        }
        Evaluacion evaluacion = evaluar(proyecto, request, false);
        return new VerificacionResponse(request.emergenciaRef(), evaluacion.factible(), evaluacion.items());
    }

    // ---------- Compromiso (todo o nada, con locking) ----------

    @Transactional
    public CompromisoResponse comprometer(CompromisoRequest request) {
        // Paso 1: proyecto de la emergencia (se crea con el primer compromiso) bloqueado
        proyectoRepository.crearSiNoExiste(request.emergenciaRef(), request.gravedad().name(),
                request.descripcion(), LocalDateTime.now());
        Proyecto proyecto = proyectoRepository.findForUpdate(request.emergenciaRef()).orElseThrow();
        if (proyecto.estaCerrado()) {
            throw proyectoCerrado(proyecto);
        }

        Evaluacion evaluacion = evaluar(proyecto, request, true);
        if (!evaluacion.factible()) {
            throw new ConflictoRecursosException(evaluacion.items());
        }

        List<Compromiso> resultado = new ArrayList<>(evaluacion.existentes());
        for (Aprobado a : evaluacion.aprobados()) {
            a.inventario().comprometer(a.item().cantidad());
            resultado.add(compromisoRepository.save(
                    new Compromiso(proyecto, a.item().ofertaRef(), a.inventario(), a.item().cantidad(), a.rol())));
        }

        return new CompromisoResponse(proyecto.getEmergenciaRef(), proyecto.getEstado().name(),
                evaluacion.aprobados().size(), evaluacion.existentes().size(),
                resultado.stream().map(CompromisoDTO::desde).toList());
    }

    private Evaluacion evaluar(Proyecto proyecto, CompromisoRequest request, boolean bloquear) {
        List<Item> items = aplanar(request);

        // Paso 2: ids de las filas de inventario (sin cargar las entidades)
        Map<Item, Long> idInventario = new HashMap<>();
        for (Item item : items) {
            if (item.cuitValido()) {
                inventarioRepository.findIdByCuitAndTipo(item.cuit(), item.tipoRecurso())
                        .ifPresent(id -> idInventario.put(item, id));
            }
        }

        // Paso 3: bloqueo (o lectura simple en el dry-run)
        List<Long> ids = idInventario.values().stream().distinct().sorted().toList();
        List<RecursoInventario> filas = ids.isEmpty() ? List.of()
                : bloquear ? inventarioRepository.findAllByIdForUpdate(ids) : inventarioRepository.findAllById(ids);
        Map<Long, RecursoInventario> inventarios = new HashMap<>();
        filas.forEach(r -> inventarios.put(r.getId(), r));

        // Lo que queda disponible en cada fila a medida que se asignan aportes del mismo request
        Map<Long, Integer> restante = new HashMap<>();
        filas.forEach(r -> restante.put(r.getId(), r.getDisponible()));

        // Paso 4: restricciones
        List<TipoRecurso> tipos = tipoRecursoRepository.findAll();
        List<ItemVerificacion> resultado = new ArrayList<>();
        List<Aprobado> aprobados = new ArrayList<>();
        List<Compromiso> existentes = new ArrayList<>();

        for (Item item : items) {
            Long invId = idInventario.get(item);
            int disponible = invId == null ? 0 : restante.get(invId);

            // Reintento: esta ONG ya tiene comprometida esta oferta en este proyecto
            Optional<Compromiso> previo = proyecto == null || !item.cuitValido() ? Optional.empty()
                    : compromisoRepository.findByProyectoAndOfertaRefAndOngCuit(proyecto, item.ofertaRef(), item.cuit());
            if (previo.isPresent()) {
                existentes.add(previo.get());
                resultado.add(ok(item, disponible, "Ya estaba comprometido (" + previo.get().getEstado() + ")"));
                continue;
            }

            String problema = null;
            RolAsignado rol = null;
            Optional<TipoRecurso> tipo = tipos.stream().filter(t -> t.getCodigo().equals(item.tipoRecurso())).findFirst();
            Ong ong = item.cuitValido() ? ongRepository.findByCuit(item.cuit()).orElse(null) : null;

            if (!item.cuitValido()) {
                problema = "CUIT inválido";
            } else if (tipo.isEmpty()) {
                problema = "El tipo de recurso no está en el catálogo nacional";
            } else if (ong == null) {
                problema = "La ONG no está registrada en el Sistema Nacional";
            } else {
                PerfilCompetencia perfil = evaluador.evaluar(item.cuit(), ong, tipo.get(), item.cantidad(),
                        request.gravedad(), tipos);
                rol = perfil.rolAsignado();
                if (rol == RolAsignado.SIN_HABILITACION_VIGENTE) {
                    problema = String.join("; ", perfil.observaciones());
                } else if (invId == null) {
                    problema = "La ONG no declaró inventario de " + item.tipoRecurso();
                } else if (item.cantidad() > disponible) {
                    problema = "Pide " + item.cantidad() + " pero hay " + disponible + " disponibles";
                }
            }

            if (problema != null) {
                resultado.add(new ItemVerificacion(item.ofertaRef(), item.cuit(), item.tipoRecurso(),
                        item.cantidad(), disponible, false, problema));
            } else {
                restante.put(invId, disponible - item.cantidad());
                aprobados.add(new Aprobado(item, inventarios.get(invId), rol));
                resultado.add(ok(item, disponible, rol == RolAsignado.PRINCIPAL ? null : "Participa como " + rol));
            }
        }

        return new Evaluacion(resultado, aprobados, existentes);
    }

    private static List<Item> aplanar(CompromisoRequest request) {
        List<Item> items = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (OfertaRequest oferta : request.ofertas()) {
            for (AporteRequest aporte : oferta.aportes()) {
                boolean valido = Cuit.esValido(aporte.cuit());
                String cuit = valido ? Cuit.normalizar(aporte.cuit()) : aporte.cuit();
                if (!vistos.add(oferta.ofertaRef() + "|" + cuit)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La ONG " + cuit + " aparece dos veces en la oferta " + oferta.ofertaRef());
                }
                items.add(new Item(oferta.ofertaRef(), cuit, valido, oferta.tipoRecurso().toUpperCase(), aporte.cantidad()));
            }
        }
        return items;
    }

    private static ItemVerificacion ok(Item item, int disponible, String motivo) {
        return new ItemVerificacion(item.ofertaRef(), item.cuit(), item.tipoRecurso(), item.cantidad(),
                disponible, true, motivo);
    }

    // ---------- Liberación y cierre ----------

    // La ONG terminó sus actividades en la emergencia: se liberan todos sus compromisos activos
    @Transactional
    public LiberacionResponse finalizarOng(String emergenciaRef, String cuit) {
        return liberar(emergenciaRef, Cuit.normalizar(cuit), false);
    }

    // Cierre del proyecto: se libera todo lo que quede activo y no admite más compromisos
    @Transactional
    public LiberacionResponse cerrarProyecto(String emergenciaRef) {
        return liberar(emergenciaRef, null, true);
    }

    // Mismo orden de bloqueo que al comprometer: proyecto → filas de inventario por id
    private LiberacionResponse liberar(String emergenciaRef, String cuit, boolean cerrar) {
        Proyecto proyecto = proyectoRepository.findForUpdate(emergenciaRef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay recursos comprometidos para la emergencia " + emergenciaRef));

        List<Long> ids = compromisoRepository.findInventarioIdsActivos(proyecto, cuit).stream().sorted().toList();
        if (!ids.isEmpty()) {
            inventarioRepository.findAllByIdForUpdate(ids);
        }

        List<Compromiso> activos = compromisoRepository.findActivos(proyecto, cuit);
        activos.forEach(Compromiso::liberar);

        if (cerrar && !proyecto.estaCerrado()) {
            proyecto.cerrar();
        }

        return new LiberacionResponse(proyecto.getEmergenciaRef(), cuit, proyecto.getEstado().name(),
                activos.stream().map(CompromisoDTO::desde).toList());
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public ProyectoDTO proyecto(String emergenciaRef) {
        return ProyectoDTO.desde(proyectoRepository.findByEmergenciaRef(emergenciaRef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay recursos comprometidos para la emergencia " + emergenciaRef)));
    }

    @Transactional(readOnly = true)
    public List<CompromisoDTO> buscar(String emergenciaRef, String cuit, EstadoCompromiso estado) {
        String cuitNormalizado = cuit == null ? null : Cuit.normalizar(cuit);
        return compromisoRepository.buscar(emergenciaRef, cuitNormalizado, estado).stream()
                .map(CompromisoDTO::desde)
                .toList();
    }

    private static ResponseStatusException proyectoCerrado(Proyecto proyecto) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "El proyecto de la emergencia " + proyecto.getEmergenciaRef() + " ya está cerrado");
    }
}
