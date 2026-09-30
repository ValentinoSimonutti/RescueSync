package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.NuevaOfertaDTO;
import ar.edu.unlp.rescuesync.dto.OfertaDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.ONG;
import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import ar.edu.unlp.rescuesync.repository.ONGRepository;
import ar.edu.unlp.rescuesync.repository.OfertaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final ONGRepository ongRepository;
    private final LoteRepository loteRepository;

    public OfertaService(
            OfertaRepository ofertaRepository,
            ONGRepository ongRepository,
            LoteRepository loteRepository) {

        this.ofertaRepository = ofertaRepository;
        this.ongRepository = ongRepository;
        this.loteRepository = loteRepository;
    }

    // La ONG logueada ofrece ayuda para un lote.
    // Solo Postgres: Bonita NO se toca (el timer de la ventana sigue corriendo).
    @Transactional
    public OfertaDTO crearOferta(NuevaOfertaDTO dto, UsuarioLogueado usuario) {

        // 1. La ONG sale de la sesión (nunca de un parámetro: nadie oferta en nombre de otra)
        ONG ong = ongRepository.findById(usuario.organizacionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "El usuario no pertenece a una ONG"));

        // 2. El lote tiene que existir
        if (dto.getLoteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el lote");
        }
        Lote lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el lote " + dto.getLoteId()));

        // 3. La convocatoria tiene que estar abierta y dentro de la ventana de tiempo
        Emergencia emergencia = lote.getEmergencia();
        if (emergencia.getEstado() != EstadoEmergencia.EN_CONVOCATORIA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La convocatoria no está abierta");
        }
        if (emergencia.getFechaCierreOfertas() != null
                && LocalDateTime.now().isAfter(emergencia.getFechaCierreOfertas())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ventana de ofertas ya cerró");
        }

        // 4. Oferta parcial permitida: entre 1 y lo que pide el lote
        if (dto.getCantidad() < 1 || dto.getCantidad() > lote.getCantidad()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad debe estar entre 1 y " + lote.getCantidad());
        }

        // 5. Guardar: el recurso es el del lote
        Oferta oferta = new Oferta(dto.getCantidad(), lote.getRecurso(), ong, lote);
        return convertirADTO(ofertaRepository.save(oferta));
    }

    private OfertaDTO convertirADTO(Oferta oferta) {
        OfertaDTO dto = new OfertaDTO();

        dto.setId(oferta.getId());
        dto.setCantidad(oferta.getCantidad());
        dto.setRecurso(oferta.getRecurso());
        dto.setOngId(oferta.getOng().getId());
        dto.setLoteId(oferta.getLote().getId());

        return dto;
    }
}
