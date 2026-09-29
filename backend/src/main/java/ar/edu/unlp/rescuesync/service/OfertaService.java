package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.dto.OfertaDTO;
import ar.edu.unlp.rescuesync.dto.NuevaOfertaDTO;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.entity.ONG;
import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import ar.edu.unlp.rescuesync.repository.ONGRepository;
import ar.edu.unlp.rescuesync.repository.OfertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public OfertaDTO crearOferta(NuevaOfertaDTO dto, Long ongId, Long loteId) {

        ONG ong = ongRepository
                .findById(ongId)
                .orElseThrow();

        Lote lote = loteRepository
                .findById(loteId)
                .orElseThrow();

        Oferta oferta = new Oferta(
                dto.getCantidad(),
                dto.getRecurso(),
                ong,
                lote
        );

        Oferta guardada = ofertaRepository.save(oferta);

        return convertirADTO(guardada);
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
