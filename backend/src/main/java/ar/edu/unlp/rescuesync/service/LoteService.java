package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.dto.LoteDTO;
import ar.edu.unlp.rescuesync.dto.NuevoLoteDTO;
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
    public LoteDTO crearLote(NuevoLoteDTO dto, Long emergenciaId) {

        Emergencia emergencia = emergenciaRepository
                .findById(emergenciaId)
                .orElseThrow();

        Lote lote = new Lote(
                dto.getRecurso(),
                dto.getCantidad(),
                emergencia
        );

        emergencia.addLote(lote);

        Lote guardado = loteRepository.save(lote);

        return convertirADTO(guardado);
    }

    private LoteDTO convertirADTO(Lote lote) {
        LoteDTO dto = new LoteDTO();

        dto.setId(lote.getId());
        dto.setRecurso(lote.getRecurso());
        dto.setCantidad(lote.getCantidad());
        dto.setEmergenciaId(lote.getEmergencia().getId());

        return dto;
    }
}
