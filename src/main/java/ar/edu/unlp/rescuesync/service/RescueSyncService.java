package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.MunicipioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RescueSyncService {

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;

    public RescueSyncService(
            EmergenciaRepository emergenciaRepository,
            MunicipioRepository municipioRepository) {

        this.emergenciaRepository = emergenciaRepository;
        this.municipioRepository = municipioRepository;
    }

    @Transactional
    public Emergencia cargarEmergencia(Emergencia e){
        Municipio m = municipioRepository
                .findById(e.getMunicipio().getId())
                .orElseThrow();

        m.cargarEmergencia(e);
        return emergenciaRepository.save(e);
    }


}
