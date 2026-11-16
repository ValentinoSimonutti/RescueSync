package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import ar.edu.unlp.rescuesync.dto.DashboardBonitaDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// Dashboard del auditor: tiempos del proceso, sacados de la API REST de Bonita.
//
// Bonita guarda en /API/bpm/archivedFlowNode un registro por cada estado que pasó una tarea
// (initializing -> ready -> completed). Para cada tarea:
//   fin    = archivedDate del registro final (completed / aborted).
//   inicio = si Bonita archivó más de un estado de la tarea, el reached_state_date más viejo;
//            si no, el fin del elemento anterior del mismo caso (el proceso es secuencial),
//            o el inicio del caso si es la primera.
// La etapa "Convocatoria y ofertas" dura lo que dura la ventana: el timer interrumpe
// "Cargar o editar ofertas".
@Service
public class DashboardBonitaService {

    // Tarea del diagrama -> etapa del enunciado (1 a 7) y quién la resuelve
    private record InfoTarea(int etapa, String responsable) {
    }

    private static final Map<String, InfoTarea> TAREAS = new LinkedHashMap<>();
    private static final List<String> ETAPAS = List.of(
            "Registro de emergencia",
            "Generación de lotes",
            "Convocatoria y ofertas",
            "Validación externa",
            "Adjudicación y notificación",
            "Compromiso nacional",
            "Ejecución y cierre");

    static {
        TAREAS.put("Registrar Emergencia", new InfoTarea(1, "Municipio"));
        TAREAS.put("Generar lotes", new InfoTarea(2, "Centro Coordinador"));
        TAREAS.put("Publicar Convocatoria", new InfoTarea(2, "Bonita"));
        TAREAS.put("Cargar o editar ofertas", new InfoTarea(3, "ONGs"));
        TAREAS.put("Obtener ofertas", new InfoTarea(4, "Bonita → App local"));
        TAREAS.put("Evaluar Ofertas", new InfoTarea(4, "Bonita → Sistema Nacional"));
        TAREAS.put("Evaluar curso de acción alternativo", new InfoTarea(4, "Centro Coordinador"));
        TAREAS.put("Adjudicar y notificar", new InfoTarea(5, "Municipio"));
        TAREAS.put("Comprometer Recursos", new InfoTarea(6, "Bonita → Sistema Nacional"));
        TAREAS.put("Monitorear estado de despliegue", new InfoTarea(7, "Centro Coordinador"));
        TAREAS.put("Ejecutar y marcar tareas finalizadas", new InfoTarea(7, "ONGs"));
    }

    // "aborted": la tarea "Cargar o editar ofertas" la corta el timer de la ventana
    private static final Set<String> ESTADOS_FINALES = Set.of("completed", "aborted", "cancelled", "skipped");

    // Un elemento ya ejecutado del diagrama (agrupa los registros archivados de una misma instancia)
    private record Nodo(String nombre, String tipo, long caseId, LocalDateTime inicioArchivado, LocalDateTime fin) {
        boolean esTarea() {
            return tipo.endsWith("_TASK");
        }
    }

    // Una tarea con su duración ya calculada
    private record Ejecucion(String nombre, String tipo, long caseId, double minutos) {
    }

    private final BonitaClient bonita;
    private final EmergenciaRepository emergenciaRepository;

    public DashboardBonitaService(BonitaClient bonita, EmergenciaRepository emergenciaRepository) {
        this.bonita = bonita;
        this.emergenciaRepository = emergenciaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardBonitaDTO indicadores(BonitaSession sesion, LocalDate desde, LocalDate hasta) {

        List<Map<String, Object>> procesos = bonita.procesosDesplegados(sesion);
        if (procesos.isEmpty()) {
            return new DashboardBonitaDTO(0, 0, null, List.of(), List.of(), List.of(), List.of(),
                    "El proceso RescueSync no está desplegado en Bonita.");
        }

        // inicio y fin de cada caso (fin = null si sigue activo)
        Map<Long, LocalDateTime> inicioCaso = new HashMap<>();
        Map<Long, LocalDateTime> finCaso = new HashMap<>();
        List<Map<String, Object>> nodosCrudos = new ArrayList<>();
        List<Map<String, Object>> pendientesCrudos = new ArrayList<>();

        for (Map<String, Object> proceso : procesos) {
            String processId = texto(proceso.get("id"));
            for (Map<String, Object> c : bonita.casosActivos(sesion, processId)) {
                inicioCaso.put(numero(c.get("id")), fecha(c.get("start")));
            }
            for (Map<String, Object> c : bonita.casosArchivados(sesion, processId)) {
                long id = numero(c.getOrDefault("sourceObjectId", c.get("id")));
                inicioCaso.put(id, fecha(c.get("start")));
                finCaso.put(id, fecha(c.get("end_date")));
            }
            nodosCrudos.addAll(bonita.nodosArchivados(sesion, processId));
            pendientesCrudos.addAll(bonita.tareasListas(sesion, processId));
        }

        // Filtro por fecha de inicio del caso
        Set<Long> casos = inicioCaso.entrySet().stream()
                .filter(e -> dentroDelRango(e.getValue(), desde, hasta))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Ejecucion> ejecuciones = calcularDuraciones(nodosCrudos, casos, inicioCaso);
        List<Map<String, Object>> pendientes = pendientesCrudos.stream()
                .filter(t -> casos.contains(caseIdDe(t)))
                .toList();

        // ---- Casos -----------------------------------------------------------
        Map<Long, Emergencia> emergencias = emergenciaRepository.findAll().stream()
                .filter(e -> e.getBonitaCaseId() != null)
                .collect(Collectors.toMap(Emergencia::getBonitaCaseId, e -> e, (a, b) -> b));

        Map<Long, String> tareaActual = pendientes.stream()
                .collect(Collectors.groupingBy(this::caseIdDe, TreeMap::new,
                        Collectors.mapping(t -> texto(t.get("name")), Collectors.joining(", "))));

        LocalDateTime ahora = LocalDateTime.now();
        List<DashboardBonitaDTO.Caso> listaCasos = casos.stream()
                .map(id -> {
                    LocalDateTime inicio = inicioCaso.get(id);
                    LocalDateTime fin = finCaso.get(id);
                    boolean terminado = finCaso.containsKey(id);
                    Emergencia e = emergencias.get(id);
                    return new DashboardBonitaDTO.Caso(id,
                            e == null ? null : e.getId(),
                            e == null ? null : e.getZonaAfectada(),
                            terminado ? "Finalizado" : "En curso",
                            inicio, fin,
                            minutos(inicio, terminado ? fin : ahora),
                            terminado ? null : tareaActual.getOrDefault(id, "Tarea automática de Bonita"));
                })
                .sorted(Comparator.comparingLong(DashboardBonitaDTO.Caso::caseId).reversed())
                .toList();

        int finalizados = (int) listaCasos.stream().filter(c -> c.estado().equals("Finalizado")).count();
        Double duracionPromedio = promedio(listaCasos.stream()
                .filter(c -> c.estado().equals("Finalizado"))
                .map(DashboardBonitaDTO.Caso::duracionMin)
                .toList());

        String advertencia = casos.isEmpty()
                ? "Bonita no tiene casos en este período. Cada Run de Studio vuelve a desplegar el proceso y borra los casos."
                : null;

        return new DashboardBonitaDTO(listaCasos.size() - finalizados, finalizados, duracionPromedio,
                etapas(ejecuciones), tareas(ejecuciones), pendientes(pendientes, ahora), listaCasos, advertencia);
    }

    // ---- Duraciones ------------------------------------------------------------

    private List<Ejecucion> calcularDuraciones(List<Map<String, Object>> crudos, Set<Long> casos,
                                               Map<Long, LocalDateTime> inicioCaso) {

        // Una instancia de tarea puede tener varios registros archivados (uno por estado)
        Map<String, List<Map<String, Object>>> porInstancia = crudos.stream()
                .filter(n -> casos.contains(caseIdDe(n)))
                .collect(Collectors.groupingBy(n -> texto(n.getOrDefault("sourceObjectId", n.get("id")))));

        List<Nodo> nodos = new ArrayList<>();
        for (List<Map<String, Object>> registros : porInstancia.values()) {
            Map<String, Object> primero = registros.get(0);
            // Bonita también archiva los estados intermedios (initializing, ready...) de tareas que
            // siguen activas: solo cuentan las que llegaron a un estado final
            LocalDateTime fin = registros.stream()
                    .filter(r -> ESTADOS_FINALES.contains(texto(r.get("state"))))
                    .map(r -> fecha(r.get("archivedDate")))
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            if (fin == null) continue;
            LocalDateTime inicio = registros.size() < 2 ? null
                    : registros.stream().map(r -> fecha(r.get("reached_state_date")))
                    .filter(Objects::nonNull).min(Comparator.naturalOrder())
                    .filter(f -> f.isBefore(fin)).orElse(null);
            nodos.add(new Nodo(texto(primero.get("name")), texto(primero.get("type")), caseIdDe(primero), inicio, fin));
        }

        List<Ejecucion> ejecuciones = new ArrayList<>();
        Map<Long, List<Nodo>> porCaso = nodos.stream().collect(Collectors.groupingBy(Nodo::caseId));

        porCaso.forEach((caseId, delCaso) -> {
            delCaso.sort(Comparator.comparing(Nodo::fin));
            for (Nodo nodo : delCaso) {
                if (!nodo.esTarea()) continue;
                LocalDateTime inicio = nodo.inicioArchivado();
                if (inicio == null) {
                    // fin del elemento anterior (los eventos de borde terminan junto con la tarea que cortan)
                    inicio = delCaso.stream()
                            .filter(n -> n != nodo && n.fin().isBefore(nodo.fin()) && !n.tipo().contains("BOUNDARY"))
                            .map(Nodo::fin)
                            .max(Comparator.naturalOrder())
                            .orElse(inicioCaso.get(caseId));
                }
                Double min = minutos(inicio, nodo.fin());
                if (min != null) {
                    ejecuciones.add(new Ejecucion(nodo.nombre(), nodo.tipo(), caseId, min));
                }
            }
        });
        return ejecuciones;
    }

    private List<DashboardBonitaDTO.Etapa> etapas(List<Ejecucion> ejecuciones) {
        List<DashboardBonitaDTO.Etapa> etapas = new ArrayList<>();
        for (int i = 0; i < ETAPAS.size(); i++) {
            int etapa = i + 1;
            // minutos de la etapa en cada caso (si una tarea se repitió, por ejemplo al reabrir, se suma)
            Map<Long, Double> porCaso = ejecuciones.stream()
                    .filter(e -> TAREAS.containsKey(e.nombre()) && TAREAS.get(e.nombre()).etapa() == etapa)
                    .collect(Collectors.groupingBy(Ejecucion::caseId, Collectors.summingDouble(Ejecucion::minutos)));
            Collection<Double> valores = porCaso.values();
            etapas.add(new DashboardBonitaDTO.Etapa(etapa, ETAPAS.get(i), valores.size(),
                    promedio(valores),
                    valores.stream().min(Double::compare).orElse(null),
                    valores.stream().max(Double::compare).orElse(null)));
        }
        return etapas;
    }

    private List<DashboardBonitaDTO.Tarea> tareas(List<Ejecucion> ejecuciones) {
        Map<String, List<Ejecucion>> porNombre = ejecuciones.stream()
                .collect(Collectors.groupingBy(Ejecucion::nombre));

        // Primero las del diagrama en orden, después cualquier otra que aparezca
        List<String> orden = new ArrayList<>(TAREAS.keySet());
        porNombre.keySet().stream().filter(n -> !TAREAS.containsKey(n)).sorted().forEach(orden::add);

        return orden.stream()
                .filter(porNombre::containsKey)
                .map(nombre -> {
                    List<Ejecucion> lista = porNombre.get(nombre);
                    InfoTarea info = TAREAS.get(nombre);
                    List<Double> mins = lista.stream().map(Ejecucion::minutos).toList();
                    return new DashboardBonitaDTO.Tarea(nombre,
                            info == null ? "Otra" : ETAPAS.get(info.etapa() - 1),
                            info == null ? "—" : info.responsable(),
                            lista.get(0).tipo().equals("AUTOMATIC_TASK") ? "Automática" : "Humana",
                            lista.size(), promedio(mins),
                            mins.stream().max(Double::compare).orElse(null));
                })
                .toList();
    }

    private List<DashboardBonitaDTO.Pendiente> pendientes(List<Map<String, Object>> listas, LocalDateTime ahora) {
        return listas.stream()
                .collect(Collectors.groupingBy(t -> texto(t.get("name")), LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    InfoTarea info = TAREAS.get(e.getKey());
                    List<Double> esperas = e.getValue().stream()
                            .map(t -> minutos(fecha(t.get("reached_state_date")), ahora))
                            .filter(Objects::nonNull).toList();
                    return new DashboardBonitaDTO.Pendiente(e.getKey(),
                            info == null ? "—" : info.responsable(), e.getValue().size(), promedio(esperas));
                })
                .sorted(Comparator.comparingLong(DashboardBonitaDTO.Pendiente::cantidad).reversed())
                .toList();
    }

    // ---- helpers -----------------------------------------------------------------

    private long caseIdDe(Map<String, Object> nodo) {
        Object id = nodo.get("rootCaseId");
        if (id == null || texto(id).isBlank()) id = nodo.get("caseId");
        return numero(id);
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private static long numero(Object valor) {
        try {
            return Long.parseLong(texto(valor));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Bonita manda las fechas como "2026-11-16 15:04:05.123" (vacío si no tiene)
    private static LocalDateTime fecha(Object valor) {
        String s = texto(valor).trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Double minutos(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) return null;
        return Math.max(0, Duration.between(desde, hasta).toMillis()) / 60000.0;
    }

    private static Double promedio(Collection<Double> valores) {
        OptionalDouble prom = valores.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return prom.isPresent() ? prom.getAsDouble() : null;
    }

    private static boolean dentroDelRango(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return true;
        if (fecha == null) return false;
        LocalDate dia = fecha.toLocalDate();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }
}
