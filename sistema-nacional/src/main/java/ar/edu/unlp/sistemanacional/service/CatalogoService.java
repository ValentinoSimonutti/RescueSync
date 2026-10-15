package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.CertificacionDTO;
import ar.edu.unlp.sistemanacional.dto.DisponibilidadDTO;
import ar.edu.unlp.sistemanacional.dto.DisponibilidadDTO.DisponibilidadOngDTO;
import ar.edu.unlp.sistemanacional.dto.TipoRecursoDTO;
import ar.edu.unlp.sistemanacional.entity.RecursoInventario;
import ar.edu.unlp.sistemanacional.entity.TipoRecurso;
import ar.edu.unlp.sistemanacional.repository.CertificacionRepository;
import ar.edu.unlp.sistemanacional.repository.RecursoInventarioRepository;
import ar.edu.unlp.sistemanacional.repository.TipoRecursoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

// Catálogo nacional (certificaciones y tipos de recurso) y disponibilidad global
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final CertificacionRepository certificacionRepository;
    private final TipoRecursoRepository tipoRecursoRepository;
    private final RecursoInventarioRepository inventarioRepository;

    public CatalogoService(CertificacionRepository certificacionRepository,
                           TipoRecursoRepository tipoRecursoRepository,
                           RecursoInventarioRepository inventarioRepository) {
        this.certificacionRepository = certificacionRepository;
        this.tipoRecursoRepository = tipoRecursoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public List<CertificacionDTO> certificaciones() {
        List<TipoRecurso> tipos = tipoRecursoRepository.findAll();
        return certificacionRepository.findAll().stream()
                .map(c -> new CertificacionDTO(
                        c.getCodigo(),
                        c.getNombre(),
                        c.getDescripcion(),
                        tipos.stream()
                                .filter(t -> t.getCertificacionRequerida() != null
                                        && t.getCertificacionRequerida().getId().equals(c.getId()))
                                .map(TipoRecurso::getCodigo)
                                .toList()))
                .toList();
    }

    public List<TipoRecursoDTO> tiposRecurso() {
        return tipoRecursoRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoRecurso::getNombre))
                .map(TipoRecursoDTO::desde)
                .toList();
    }

    // codigoTipoRecurso null = todos los tipos
    public List<DisponibilidadDTO> disponibilidad(String codigoTipoRecurso) {
        List<TipoRecurso> tipos;
        if (codigoTipoRecurso == null) {
            tipos = tipoRecursoRepository.findAll();
        } else {
            tipos = List.of(tipoRecursoRepository.findByCodigo(codigoTipoRecurso.toUpperCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe el tipo de recurso " + codigoTipoRecurso)));
        }

        return tipos.stream()
                .sorted(Comparator.comparing(TipoRecurso::getNombre))
                .map(this::disponibilidadDe)
                .toList();
    }

    private DisponibilidadDTO disponibilidadDe(TipoRecurso tipo) {
        // Solo cuentan las ONGs habilitadas: una pendiente o suspendida no puede ofrecer
        List<RecursoInventario> recursos = inventarioRepository.findByTipoRecurso(tipo).stream()
                .filter(r -> r.getOng().estaHabilitada())
                .sorted(Comparator.comparing(RecursoInventario::getDisponible).reversed())
                .toList();

        return new DisponibilidadDTO(
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.getUnidad(),
                recursos.stream().mapToInt(RecursoInventario::getCantidadTotal).sum(),
                recursos.stream().mapToInt(RecursoInventario::getCantidadComprometida).sum(),
                recursos.stream().mapToInt(RecursoInventario::getDisponible).sum(),
                recursos.stream()
                        .map(r -> new DisponibilidadOngDTO(r.getOng().getCuit(), r.getOng().getNombre(),
                                r.getCantidadTotal(), r.getCantidadComprometida(), r.getDisponible()))
                        .toList()
        );
    }
}
