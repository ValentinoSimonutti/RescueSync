package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.MunicipioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmergenciaService {

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;

    public EmergenciaService(
            EmergenciaRepository emergenciaRepository,
            MunicipioRepository municipioRepository) {

        this.emergenciaRepository = emergenciaRepository;
        this.municipioRepository = municipioRepository;
    }

    @Transactional
    public Emergencia registrarEmergencia(NuevaEmergenciaDTO dto){

        // TODO: tomar del usuario logueado. Por ahora dejamos el 1L directo para que siga funcionando
        Municipio m = municipioRepository
                .findById(1L)
                .orElseThrow();

        // Armamos la entidad real usando los datos que vinieron en el DTO
        Emergencia e = new Emergencia(
                dto.getZonaAfectada(),
                dto.getDescripcion(),
                dto.getTipoDesastre(),
                dto.getGravedad(),
                m
        );

        m.cargarEmergencia(e);
        return emergenciaRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<Emergencia> listarEmergencias(){
        return this.emergenciaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Emergencia buscarPorId(Long id) {
        return emergenciaRepository.findById(id)
                .orElseThrow();
    }

}
