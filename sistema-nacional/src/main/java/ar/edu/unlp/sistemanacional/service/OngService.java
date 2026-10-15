package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.*;
import ar.edu.unlp.sistemanacional.entity.*;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import ar.edu.unlp.sistemanacional.entity.enums.Rol;
import ar.edu.unlp.sistemanacional.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// Onboarding, estado, certificaciones e inventario de las ONGs
@Service
public class OngService {

    private final OngRepository ongRepository;
    private final UsuarioRepository usuarioRepository;
    private final CertificacionRepository certificacionRepository;
    private final OngCertificacionRepository ongCertificacionRepository;
    private final TipoRecursoRepository tipoRecursoRepository;
    private final RecursoInventarioRepository inventarioRepository;
    private final PasswordEncoder passwordEncoder;

    public OngService(OngRepository ongRepository, UsuarioRepository usuarioRepository,
                      CertificacionRepository certificacionRepository,
                      OngCertificacionRepository ongCertificacionRepository,
                      TipoRecursoRepository tipoRecursoRepository,
                      RecursoInventarioRepository inventarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.ongRepository = ongRepository;
        this.usuarioRepository = usuarioRepository;
        this.certificacionRepository = certificacionRepository;
        this.ongCertificacionRepository = ongCertificacionRepository;
        this.tipoRecursoRepository = tipoRecursoRepository;
        this.inventarioRepository = inventarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Onboarding y estado ----------

    @Transactional
    public OngDetalleDTO registrar(RegistroOngRequest request) {
        String cuit = Cuit.normalizar(request.cuit());

        if (ongRepository.existsByCuit(cuit)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una ONG registrada con el CUIT " + cuit);
        }
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario '" + request.username() + "' ya existe");
        }

        Ong ong = ongRepository.save(new Ong(cuit, request.nombre().trim(), request.email().trim(),
                request.provincia().trim()));
        usuarioRepository.save(new Usuario(request.username(), passwordEncoder.encode(request.password()),
                Rol.ONG, ong));

        return OngDetalleDTO.desde(ong);
    }

    @Transactional
    public OngDetalleDTO cambiarEstado(String cuit, CambioEstadoOngRequest request) {
        Ong ong = buscar(cuit);
        ong.cambiarEstado(request.estado(), request.motivo());
        return OngDetalleDTO.desde(ong);
    }

    // ---------- Consultas ----------

    // Filtros opcionales: estado, certificación vigente y tipo de recurso con disponibilidad > 0
    @Transactional(readOnly = true)
    public List<OngDTO> listar(EstadoOng estado, String certificacion, String tipoRecurso) {
        LocalDate hoy = LocalDate.now();
        return ongRepository.findAll().stream()
                .filter(o -> estado == null || o.getEstado() == estado)
                .filter(o -> certificacion == null || o.getCertificaciones().stream()
                        .anyMatch(c -> c.getCertificacion().getCodigo().equalsIgnoreCase(certificacion)
                                && c.estaVigente(hoy)))
                .filter(o -> tipoRecurso == null || o.getInventario().stream()
                        .anyMatch(r -> r.getTipoRecurso().getCodigo().equalsIgnoreCase(tipoRecurso)
                                && r.getDisponible() > 0))
                .sorted(Comparator.comparing(Ong::getNombre))
                .map(OngDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public OngDetalleDTO detalle(String cuit) {
        return OngDetalleDTO.desde(buscar(cuit));
    }

    // ---------- Certificaciones (las otorga el ADMIN) ----------

    @Transactional
    public OngDetalleDTO otorgarCertificacion(String cuit, OtorgarCertificacionRequest request) {
        Ong ong = buscar(cuit);
        Certificacion certificacion = certificacionRepository.findByCodigo(request.certificacion().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la certificación " + request.certificacion()));

        ongCertificacionRepository.findByOngAndCertificacion(ong, certificacion)
                .ifPresentOrElse(
                        existente -> existente.renovar(request.nivel(), request.vigenteHasta()),
                        () -> ongCertificacionRepository.save(
                                new OngCertificacion(ong, certificacion, request.nivel(), request.vigenteHasta())));

        return OngDetalleDTO.desde(ong);
    }

    // ---------- Inventario (lo declara la propia ONG) ----------

    @Transactional
    public InventarioDTO actualizarInventario(String cuit, String codigoTipoRecurso, int cantidadTotal) {
        Ong ong = buscar(cuit);
        TipoRecurso tipo = tipoRecursoRepository.findByCodigo(codigoTipoRecurso.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el tipo de recurso " + codigoTipoRecurso));

        RecursoInventario recurso = inventarioRepository.findByOngAndTipoRecurso(ong, tipo)
                .orElseGet(() -> inventarioRepository.save(new RecursoInventario(ong, tipo, 0)));
        recurso.actualizarTotal(cantidadTotal);

        return InventarioDTO.desde(recurso);
    }

    private Ong buscar(String cuit) {
        String normalizado = Cuit.normalizar(cuit);
        return ongRepository.findByCuit(normalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay ninguna ONG con CUIT " + normalizado));
    }
}
