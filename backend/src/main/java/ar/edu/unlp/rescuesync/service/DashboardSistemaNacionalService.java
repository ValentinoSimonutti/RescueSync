package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.dto.DashboardLocalDTO.Conteo;
import ar.edu.unlp.rescuesync.dto.DashboardSistemaNacionalDTO;
import ar.edu.unlp.rescuesync.sistemanacional.ConsultasSN;
import ar.edu.unlp.rescuesync.sistemanacional.SistemaNacionalClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Dashboard del auditor: estado de los recursos a nivel nacional (API del Sistema Nacional en la nube).
@Service
public class DashboardSistemaNacionalService {

    private final SistemaNacionalClient sistemaNacional;

    public DashboardSistemaNacionalService(SistemaNacionalClient sistemaNacional) {
        this.sistemaNacional = sistemaNacional;
    }

    public DashboardSistemaNacionalDTO indicadores() {
        List<ConsultasSN.Disponibilidad> disponibilidad = sistemaNacional.disponibilidad();
        List<ConsultasSN.Compromiso> compromisos = sistemaNacional.compromisos();
        List<ConsultasSN.Ong> ongs = sistemaNacional.ongs();

        List<DashboardSistemaNacionalDTO.Recurso> recursos = disponibilidad.stream()
                .map(d -> new DashboardSistemaNacionalDTO.Recurso(d.tipoRecurso(), d.nombre(), d.unidad(),
                        d.total(), d.comprometido(), d.disponible()))
                .sorted(Comparator.comparing(DashboardSistemaNacionalDTO.Recurso::nombre))
                .toList();

        List<ConsultasSN.Compromiso> activos = compromisos.stream()
                .filter(c -> "ACTIVO".equals(c.estado()))
                .toList();

        Map<String, Long> porEstado = new LinkedHashMap<>();
        List.of("HABILITADA", "PENDIENTE", "SUSPENDIDA").forEach(e -> porEstado.put(e, 0L));
        ongs.forEach(o -> porEstado.merge(o.estado(), 1L, Long::sum));

        return new DashboardSistemaNacionalDTO(
                recursos,
                activos.size(),
                compromisos.size() - activos.size(),
                activos.stream().mapToInt(ConsultasSN.Compromiso::cantidad).sum(),
                porEstado.get("HABILITADA").intValue(),
                ongs.size(),
                porEstado.entrySet().stream().map(e -> new Conteo(e.getKey(), e.getValue())).toList(),
                activos.stream()
                        .sorted(Comparator.comparing(ConsultasSN.Compromiso::fechaCompromiso,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(c -> new DashboardSistemaNacionalDTO.CompromisoActivo(c.emergenciaRef(),
                                c.ongNombre(), c.tipoRecurso(), c.cantidad(), c.rolAsignado(), c.fechaCompromiso()))
                        .toList()
        );
    }
}
