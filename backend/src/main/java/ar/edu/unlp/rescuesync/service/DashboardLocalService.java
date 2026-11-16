package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.dto.DashboardLocalDTO;
import ar.edu.unlp.rescuesync.dto.DashboardLocalDTO.Conteo;
import ar.edu.unlp.rescuesync.entity.Aporte;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.ResultadoOferta;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Dashboard del auditor: indicadores que salen de la base local.
//  - Suministros movilizados = lo que tienen las ofertas adjudicadas de emergencias que ya
//    pasaron la adjudicación (si el Sistema Nacional rechaza el compromiso, la adjudicación se borra).
//  - Resolución por consorcio = lotes adjudicados a una oferta con más de una ONG.
@Service
public class DashboardLocalService {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    // Estados en los que la emergencia ya tiene recursos adjudicados
    private static final Set<EstadoEmergencia> CON_ADJUDICACION =
            EnumSet.of(EstadoEmergencia.ADJUDICADA, EstadoEmergencia.EN_EJECUCION, EstadoEmergencia.CERRADA);

    private final EmergenciaRepository emergenciaRepository;

    public DashboardLocalService(EmergenciaRepository emergenciaRepository) {
        this.emergenciaRepository = emergenciaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardLocalDTO indicadores(LocalDate desde, LocalDate hasta) {

        List<Emergencia> emergencias = emergenciaRepository.findAll().stream()
                .filter(e -> dentroDelRango(e.getFechaRegistro(), desde, hasta))
                .sorted(Comparator.comparing(Emergencia::getId))
                .toList();

        // ---- Ofertas adjudicadas (lo movilizado) -----------------------------
        List<Oferta> adjudicadas = emergencias.stream()
                .filter(e -> CON_ADJUDICACION.contains(e.getEstado()))
                .flatMap(e -> lotesVigentes(e).stream())
                .flatMap(l -> l.getOfertas().stream())
                .filter(o -> o.getEstado() == EstadoOferta.ACTIVA && o.getResultado() == ResultadoOferta.ADJUDICADA)
                .toList();

        int unidades = adjudicadas.stream().mapToInt(Oferta::getCantidad).sum();
        int porConsorcio = (int) adjudicadas.stream().filter(Oferta::esConsorcio).count();

        // Cobertura: de los lotes de emergencias adjudicadas, cuántos quedaron con una oferta
        int lotesAdjudicables = emergencias.stream()
                .filter(e -> CON_ADJUDICACION.contains(e.getEstado()))
                .mapToInt(e -> lotesVigentes(e).size())
                .sum();

        List<Oferta> todasLasOfertas = emergencias.stream()
                .flatMap(e -> e.getLotes().stream())
                .flatMap(l -> l.getOfertas().stream())
                .filter(o -> o.getEstado() == EstadoOferta.ACTIVA)
                .toList();

        // ---- Proyectos finalizados -------------------------------------------
        List<DashboardLocalDTO.ProyectoFinalizado> finalizados = emergencias.stream()
                .filter(e -> e.getEstado() == EstadoEmergencia.CERRADA)
                .map(e -> proyectoFinalizado(e, adjudicadas))
                .sorted(Comparator.comparing(DashboardLocalDTO.ProyectoFinalizado::emergenciaId).reversed())
                .toList();

        int enCurso = (int) emergencias.stream().filter(e -> e.getEstado() != EstadoEmergencia.CERRADA).count();

        var resumen = new DashboardLocalDTO.Resumen(
                emergencias.size(),
                enCurso,
                finalizados.size(),
                adjudicadas.size(),
                unidades,
                porcentaje(porConsorcio, adjudicadas.size()),
                porcentaje(adjudicadas.size(), lotesAdjudicables),
                promedio(finalizados.stream().map(DashboardLocalDTO.ProyectoFinalizado::duracionHoras)),
                todasLasOfertas.size(),
                promedio(todasLasOfertas.stream()
                        .map(o -> o.getNumeroVersion() == null ? null : o.getNumeroVersion().doubleValue()))
        );

        return new DashboardLocalDTO(
                resumen,
                contar(emergencias, e -> e.getEstado().name(), nombres(EstadoEmergencia.values())),
                contar(emergencias, e -> e.getTipoDesastre() == null ? "SIN_DATO" : e.getTipoDesastre().name(), List.of()),
                contar(emergencias, e -> e.getGravedad() == null ? "SIN_DATO" : e.getGravedad().name(), List.of("ALTA", "MEDIA", "BAJA")),
                movilizadosPorMes(adjudicadas),
                movilizadosPorRecurso(adjudicadas),
                new DashboardLocalDTO.Consorcios(adjudicadas.size(), porConsorcio,
                        adjudicadas.size() - porConsorcio, porcentaje(porConsorcio, adjudicadas.size())),
                participacionOngs(adjudicadas, todasLasOfertas),
                contar(todasLasOfertas,
                        o -> o.getRolValidacion() == null ? "SIN_VALIDAR" : o.getRolValidacion().name(),
                        List.of("PRINCIPAL", "APOYO_SECUNDARIO", "SIN_HABILITACION_VIGENTE", "SIN_VALIDAR")),
                finalizados
        );
    }

    // ---- secciones ---------------------------------------------------------------

    private List<DashboardLocalDTO.MovilizadoMes> movilizadosPorMes(List<Oferta> adjudicadas) {
        // mes -> recurso -> unidades (ordenado por mes y por recurso)
        Map<String, Map<String, Integer>> porMes = new TreeMap<>();
        for (Oferta o : adjudicadas) {
            Emergencia e = o.getLote().getEmergencia();
            LocalDateTime fecha = e.getFechaAdjudicacion() != null ? e.getFechaAdjudicacion() : e.getFechaRegistro();
            if (fecha == null) continue;
            porMes.computeIfAbsent(fecha.format(MES), m -> new TreeMap<>())
                    .merge(o.getRecurso(), o.getCantidad(), Integer::sum);
        }
        List<DashboardLocalDTO.MovilizadoMes> filas = new ArrayList<>();
        porMes.forEach((mes, recursos) -> recursos.forEach((recurso, cant) ->
                filas.add(new DashboardLocalDTO.MovilizadoMes(mes, recurso, cant))));
        return filas;
    }

    private List<DashboardLocalDTO.MovilizadoRecurso> movilizadosPorRecurso(List<Oferta> adjudicadas) {
        return adjudicadas.stream()
                .collect(Collectors.groupingBy(Oferta::getRecurso, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new DashboardLocalDTO.MovilizadoRecurso(e.getKey(),
                        e.getValue().stream().mapToInt(Oferta::getCantidad).sum(), e.getValue().size()))
                .sorted(Comparator.comparingInt(DashboardLocalDTO.MovilizadoRecurso::unidades).reversed())
                .toList();
    }

    private List<DashboardLocalDTO.ParticipacionOng> participacionOngs(List<Oferta> adjudicadas, List<Oferta> ofertas) {
        Map<String, int[]> porOng = new TreeMap<>();   // [unidades, individuales, consorcio, presentadas]

        for (Oferta o : adjudicadas) {
            boolean consorcio = o.esConsorcio();
            for (Aporte a : confirmados(o)) {
                int[] fila = porOng.computeIfAbsent(a.getOng().getNombre(), k -> new int[4]);
                fila[0] += a.getCantidad();
                fila[consorcio ? 2 : 1]++;
            }
        }
        for (Oferta o : ofertas) {
            for (Aporte a : confirmados(o)) {
                porOng.computeIfAbsent(a.getOng().getNombre(), k -> new int[4])[3]++;
            }
        }
        return porOng.entrySet().stream()
                .map(e -> new DashboardLocalDTO.ParticipacionOng(e.getKey(),
                        e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]))
                .sorted(Comparator.comparingInt(DashboardLocalDTO.ParticipacionOng::unidadesMovilizadas).reversed())
                .toList();
    }

    private DashboardLocalDTO.ProyectoFinalizado proyectoFinalizado(Emergencia e, List<Oferta> adjudicadas) {
        List<Oferta> suyas = adjudicadas.stream()
                .filter(o -> o.getLote().getEmergencia().getId().equals(e.getId()))
                .toList();
        long ongs = suyas.stream().flatMap(o -> confirmados(o).stream())
                .map(a -> a.getOng().getId()).distinct().count();
        Double horas = e.getFechaRegistro() == null || e.getFechaCierre() == null ? null
                : Duration.between(e.getFechaRegistro(), e.getFechaCierre()).toSeconds() / 3600.0;

        return new DashboardLocalDTO.ProyectoFinalizado(e.getId(), e.getZonaAfectada(),
                e.getTipoDesastre() == null ? null : e.getTipoDesastre().name(),
                e.getGravedad() == null ? null : e.getGravedad().name(),
                e.getFechaRegistro(), e.getFechaCierre(), horas,
                suyas.size(), suyas.stream().mapToInt(Oferta::getCantidad).sum(), (int) ongs,
                e.isCoberturaParcial());
    }

    // ---- helpers -----------------------------------------------------------------

    private static List<Lote> lotesVigentes(Emergencia e) {
        return e.getLotes().stream().filter(Lote::isVigente).toList();
    }

    private static List<Aporte> confirmados(Oferta o) {
        return o.getAportes().stream()
                .filter(a -> a.getEstado() == EstadoAporte.CONFIRMADO && a.getCantidad() > 0)
                .toList();
    }

    // Cuenta por clave; las claves de "orden" aparecen siempre (aunque sea con 0) y primero
    private static <T> List<Conteo> contar(List<T> items, Function<T, String> clave, List<String> orden) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        orden.forEach(k -> conteo.put(k, 0L));
        items.forEach(i -> conteo.merge(clave.apply(i), 1L, Long::sum));
        return conteo.entrySet().stream().map(e -> new Conteo(e.getKey(), e.getValue())).toList();
    }

    private static List<String> nombres(Enum<?>[] valores) {
        return Arrays.stream(valores).map(Enum::name).toList();
    }

    private static boolean dentroDelRango(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return true;
        if (fecha == null) return false;
        LocalDate dia = fecha.toLocalDate();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }

    private static Double porcentaje(int parte, int total) {
        return total == 0 ? null : Math.round(parte * 1000.0 / total) / 10.0;
    }

    private static Double promedio(java.util.stream.Stream<Double> valores) {
        OptionalDouble prom = valores.filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return prom.isPresent() ? prom.getAsDouble() : null;
    }
}
