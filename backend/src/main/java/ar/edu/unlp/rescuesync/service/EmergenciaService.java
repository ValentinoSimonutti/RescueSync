package ar.edu.unlp.rescuesync.service;

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
    public Emergencia registrarEmergencia(Emergencia e){
        // queda asi como prueba inicial. dsp seria el id del municipio que se loguea
        long id = 1;
        Municipio m = municipioRepository
                .findById(id)
                .orElseThrow();

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
