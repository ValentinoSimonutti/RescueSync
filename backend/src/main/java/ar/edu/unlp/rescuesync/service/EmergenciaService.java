package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.NuevaEmergenciaDTO;
import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import ar.edu.unlp.rescuesync.repository.MunicipioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmergenciaService {

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;          // NUEVO: para crear el case en Bonita

    public EmergenciaService(
            EmergenciaRepository emergenciaRepository,
            MunicipioRepository municipioRepository,
            BonitaClient bonitaClient) {

        this.emergenciaRepository = emergenciaRepository;
        this.municipioRepository = municipioRepository;
        this.bonitaClient = bonitaClient;
    }

    // Alta de emergencia de punta a punta: Postgres + Bonita.
    // @Transactional: si algo falla (incluido Bonita), se deshace lo guardado en Postgres.
    @Transactional
    public EmergenciaDTO registrarEmergencia(NuevaEmergenciaDTO dto, UsuarioLogueado usuario) {

        // 1. El municipio sale del usuario logueado (antes estaba fijo en 1)
        Municipio m = municipioRepository
                .findById(usuario.organizacionId())
                .orElseThrow(() -> new IllegalStateException(
                        "El usuario no pertenece a un municipio válido"));

        // 2. Guardamos la emergencia en Postgres (así obtenemos su id)
        Emergencia e = new Emergencia(
                dto.getZonaAfectada(),
                dto.getDescripcion(),
                dto.getTipoDesastre(),
                dto.getGravedad(),
                m
        );
        m.cargarEmergencia(e);
        Emergencia guardada = emergenciaRepository.save(e);

        // 3. En Bonita, con la sesión del usuario que carga la emergencia:
        BonitaSession sesion = usuario.bonita();

        //    3a. crear el case (una "copia viva" del proceso para esta emergencia)
        long caseId = bonitaClient.instanciar(sesion);

        //    3b. buscar la primera tarea del case: "Registrar Emergencia"
        long taskId = bonitaClient.buscarTareaLista(sesion, caseId);

        //    3c. completarla mandando el contract -> Bonita guarda emergenciaId = id de Postgres
        //        y el case avanza a "Generar lotes" (tarea del coordinador)
        bonitaClient.ejecutarTarea(sesion, taskId, Map.of("emergenciaIdInput", guardada.getId()));

        // 4. Guardamos el vínculo: esta emergencia <-> este case de Bonita
        //    (no hace falta save: la entidad está "administrada" y JPA la actualiza al terminar la transacción)
        guardada.setBonitaCaseId(caseId);

        return convertirADTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<EmergenciaDTO> listarEmergencias() {
        return emergenciaRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmergenciaDTO buscarPorId(Long id) {
        Emergencia emergencia = emergenciaRepository.findById(id)
                .orElseThrow();

        return convertirADTO(emergencia);
    }

    private EmergenciaDTO convertirADTO(Emergencia emergencia) {
       return EmergenciaDTO.desde(emergencia);

    }

}
