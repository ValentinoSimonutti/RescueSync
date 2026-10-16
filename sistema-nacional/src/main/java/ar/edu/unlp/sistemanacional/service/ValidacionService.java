package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.*;
import ar.edu.unlp.sistemanacional.entity.Ong;
import ar.edu.unlp.sistemanacional.entity.TipoRecurso;
import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;
import ar.edu.unlp.sistemanacional.repository.OngRepository;
import ar.edu.unlp.sistemanacional.repository.TipoRecursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Validación externa de las ofertas (Bonita la llama cuando vence la convocatoria).
// Solo lee: no bloquea nada. El bloqueo se hace al comprometer (CompromisoService).
@Service
public class ValidacionService {

    private final OngRepository ongRepository;
    private final TipoRecursoRepository tipoRecursoRepository;
    private final EvaluadorCompetencia evaluador;

    public ValidacionService(OngRepository ongRepository, TipoRecursoRepository tipoRecursoRepository,
                             EvaluadorCompetencia evaluador) {
        this.ongRepository = ongRepository;
        this.tipoRecursoRepository = tipoRecursoRepository;
        this.evaluador = evaluador;
    }

    @Transactional(readOnly = true)
    public ValidacionResponse validar(ValidacionRequest request) {
        List<TipoRecurso> tipos = tipoRecursoRepository.findAll();

        List<OfertaValidada> ofertas = request.ofertas().stream()
                .map(o -> validarOferta(o, request, tipos))
                .toList();

        return new ValidacionResponse(request.emergenciaRef(), request.gravedad(), LocalDateTime.now(), ofertas);
    }

    private OfertaValidada validarOferta(OfertaRequest oferta, ValidacionRequest request, List<TipoRecurso> tipos) {
        int ofrecida = oferta.aportes().stream().mapToInt(AporteRequest::cantidad).sum();

        Optional<TipoRecurso> tipo = tipos.stream()
                .filter(t -> t.getCodigo().equalsIgnoreCase(oferta.tipoRecurso()))
                .findFirst();

        // Recurso que no está en el catálogo: no se puede evaluar, pero se informa (no se corta toda la validación)
        if (tipo.isEmpty()) {
            return new OfertaValidada(oferta.ofertaRef(), oferta.loteRef(), oferta.tipoRecurso(),
                    oferta.cantidadRequerida(), ofrecida, 0, cubre(oferta, 0),
                    RolAsignado.SIN_HABILITACION_VIGENTE,
                    List.of("El tipo de recurso " + oferta.tipoRecurso() + " no está en el catálogo nacional"),
                    List.of());
        }

        List<PerfilCompetencia> perfiles = oferta.aportes().stream()
                .map(a -> {
                    // CUIT mal formado: se informa como no registrada en lugar de cortar toda la validación
                    String cuit = Cuit.esValido(a.cuit()) ? Cuit.normalizar(a.cuit()) : a.cuit();
                    Ong ong = ongRepository.findByCuit(cuit).orElse(null);
                    return evaluador.evaluar(cuit, ong, tipo.get(), a.cantidad(), request.gravedad(), tipos);
                })
                .toList();

        int validada = perfiles.stream().filter(PerfilCompetencia::participa)
                .mapToInt(PerfilCompetencia::cantidadValidable).sum();

        return new OfertaValidada(oferta.ofertaRef(), oferta.loteRef(), tipo.get().getCodigo(),
                oferta.cantidadRequerida(), ofrecida, validada, cubre(oferta, validada),
                rolDeLaOferta(perfiles), List.of(), perfiles);
    }

    // En un consorcio alcanza con que una ONG sea principal: las demás entran como apoyo
    private static RolAsignado rolDeLaOferta(List<PerfilCompetencia> perfiles) {
        List<PerfilCompetencia> participan = perfiles.stream().filter(PerfilCompetencia::participa).toList();
        if (participan.isEmpty()) {
            return RolAsignado.SIN_HABILITACION_VIGENTE;
        }
        return participan.stream().anyMatch(p -> p.rolAsignado() == RolAsignado.PRINCIPAL)
                ? RolAsignado.PRINCIPAL
                : RolAsignado.APOYO_SECUNDARIO;
    }

    private static Boolean cubre(OfertaRequest oferta, int validada) {
        return oferta.cantidadRequerida() == null ? null : validada >= oferta.cantidadRequerida();
    }
}
