package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Lote;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoteService {

    private final LoteRepository loteRepository;
    private final EmergenciaRepository emergenciaRepository;

    public LoteService(
            LoteRepository loteRepository,
            EmergenciaRepository emergenciaRepository) {

        this.loteRepository = loteRepository;
        this.emergenciaRepository = emergenciaRepository;
    }

    @Transactional
    public Lote crearLote(Lote lote, Long emergenciaId) {

        Emergencia emergencia = emergenciaRepository
                .findById(emergenciaId)
                .orElseThrow();

        emergencia.addLote(lote);
        return loteRepository.save(lote);
    }
}
