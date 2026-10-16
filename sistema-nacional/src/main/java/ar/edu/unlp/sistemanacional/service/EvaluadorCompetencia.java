package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.PerfilCompetencia;
import ar.edu.unlp.sistemanacional.entity.*;
import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Reglas de la validación por niveles de competencia (sin rechazo binario).
// La usan la validación (POST /validaciones/ofertas) y el compromiso (que exige que el rol no sea
// SIN_HABILITACION_VIGENTE).
//
//   ONG no registrada / pendiente / suspendida            → SIN_HABILITACION_VIGENTE
//   el recurso no requiere certificación                  → PRINCIPAL
//   tiene la certificación vigente y su nivel cubre la gravedad → PRINCIPAL
//   tiene la certificación vigente pero de nivel menor    → APOYO_SECUNDARIO
//   tiene la certificación pero vencida                   → APOYO_SECUNDARIO
//   no tiene la certificación                             → SIN_HABILITACION_VIGENTE
//
// Nivel de certificación → riesgo máximo en el que puede ser responsable:
//   BASICO → BAJA, INTERMEDIO → MEDIA, AVANZADO → ALTA
// Aparte del rol, la cantidad validable es lo que tiene disponible a nivel nacional.
@Component
public class EvaluadorCompetencia {

    public static NivelRiesgo riesgoMaximo(NivelCompetencia nivel) {
        return switch (nivel) {
            case BASICO -> NivelRiesgo.BAJA;
            case INTERMEDIO -> NivelRiesgo.MEDIA;
            case AVANZADO -> NivelRiesgo.ALTA;
        };
    }

    // ong null = el CUIT no está registrado en el Sistema Nacional.
    // todosLosTipos: el catálogo, para calcular las capacidades habilitadas.
    public PerfilCompetencia evaluar(String cuit, Ong ong, TipoRecurso tipo, int cantidad,
                                     NivelRiesgo gravedad, List<TipoRecurso> todosLosTipos) {
        List<String> obs = new ArrayList<>();

        if (ong == null) {
            obs.add("El CUIT no está registrado en el Sistema Nacional: la ONG tiene que hacer el onboarding");
            return new PerfilCompetencia(cuit, null, "NO_REGISTRADA", cantidad, 0, 0,
                    codigoCertificacion(tipo), null, null, List.of(), RolAsignado.SIN_HABILITACION_VIGENTE, obs);
        }

        LocalDate hoy = LocalDate.now();
        int disponible = ong.getInventario().stream()
                .filter(r -> r.getTipoRecurso().getId().equals(tipo.getId()))
                .mapToInt(RecursoInventario::getDisponible)
                .findFirst()
                .orElse(0);
        List<String> capacidades = capacidadesHabilitadas(ong, todosLosTipos, hoy);

        // --- Rol según estado de la ONG y certificación ---
        RolAsignado rol;
        NivelCompetencia nivel = null;
        NivelRiesgo riesgoMaximo = null;
        Certificacion requerida = tipo.getCertificacionRequerida();
        Optional<OngCertificacion> certificacion = requerida == null ? Optional.empty()
                : ong.getCertificaciones().stream()
                    .filter(c -> c.getCertificacion().getId().equals(requerida.getId()))
                    .findFirst();

        if (!ong.estaHabilitada()) {
            rol = RolAsignado.SIN_HABILITACION_VIGENTE;
            obs.add("La ONG está " + ong.getEstado() + " en el Sistema Nacional"
                    + (ong.getMotivoEstado() == null ? "" : " (" + ong.getMotivoEstado() + ")"));
        } else if (requerida == null) {
            rol = RolAsignado.PRINCIPAL;
            riesgoMaximo = NivelRiesgo.ALTA;
        } else if (certificacion.isEmpty()) {
            rol = RolAsignado.SIN_HABILITACION_VIGENTE;
            obs.add("No tiene la certificación " + requerida.getCodigo() + " que requiere " + tipo.getCodigo());
        } else {
            OngCertificacion cert = certificacion.get();
            nivel = cert.getNivel();
            riesgoMaximo = riesgoMaximo(nivel);
            if (!cert.estaVigente(hoy)) {
                rol = RolAsignado.APOYO_SECUNDARIO;
                obs.add("La certificación " + requerida.getCodigo() + " venció el " + cert.getVigenteHasta()
                        + ": solo puede participar como apoyo hasta renovarla");
            } else if (riesgoMaximo.compareTo(gravedad) >= 0) {
                rol = RolAsignado.PRINCIPAL;
            } else {
                rol = RolAsignado.APOYO_SECUNDARIO;
                obs.add("Nivel " + nivel + " en " + requerida.getCodigo() + " habilita hasta riesgo " + riesgoMaximo
                        + "; la emergencia es de gravedad " + gravedad + ": participa como apoyo");
            }
        }

        // --- Cantidad según disponibilidad nacional ---
        int validable = rol == RolAsignado.SIN_HABILITACION_VIGENTE ? 0 : Math.min(cantidad, disponible);
        if (rol != RolAsignado.SIN_HABILITACION_VIGENTE && disponible < cantidad) {
            obs.add(disponible == 0
                    ? "No tiene " + tipo.getCodigo() + " disponibles a nivel nacional (declarados o ya comprometidos)"
                    : "Ofrece " + cantidad + " pero tiene " + disponible + " disponibles a nivel nacional");
        }

        return new PerfilCompetencia(ong.getCuit(), ong.getNombre(), ong.getEstado().name(), cantidad, disponible,
                validable, codigoCertificacion(tipo), nivel, riesgoMaximo, capacidades, rol, obs);
    }

    // Tipos de recurso que la ONG puede ofrecer como principal (al menos en gravedad BAJA):
    // los que no piden certificación y los de sus certificaciones vigentes
    private List<String> capacidadesHabilitadas(Ong ong, List<TipoRecurso> todosLosTipos, LocalDate hoy) {
        if (!ong.estaHabilitada()) {
            return List.of();
        }
        return todosLosTipos.stream()
                .filter(t -> t.getCertificacionRequerida() == null
                        || ong.getCertificaciones().stream().anyMatch(c -> c.estaVigente(hoy)
                            && c.getCertificacion().getId().equals(t.getCertificacionRequerida().getId())))
                .map(TipoRecurso::getCodigo)
                .sorted()
                .toList();
    }

    private static String codigoCertificacion(TipoRecurso tipo) {
        return tipo.getCertificacionRequerida() == null ? null : tipo.getCertificacionRequerida().getCodigo();
    }
}
