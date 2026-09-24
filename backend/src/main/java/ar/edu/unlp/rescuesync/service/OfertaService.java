package ar.edu.unlp.rescuesync.service;

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
    public Oferta crearOferta(Oferta oferta, Long ongId, Long loteId) {

        ONG ong = ongRepository
                .findById(ongId)
                .orElseThrow();

        Lote lote = loteRepository
                .findById(loteId)
                .orElseThrow();

        oferta.setOng(ong);
        oferta.setLote(lote);
        return ofertaRepository.save(oferta);
    }
}
