package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.dto.EmergenciaDTO;
import ar.edu.unlp.rescuesync.dto.TareaDTO;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TareaService {

    private final BonitaClient bonitaClient;
    private final EmergenciaRepository emergenciaRepository;

    public TareaService(BonitaClient bonitaClient, EmergenciaRepository emergenciaRepository) {
        this.bonitaClient = bonitaClient;
        this.emergenciaRepository = emergenciaRepository;
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> tareasDe(UsuarioLogueado usuario) {

        // 1. Bonita: tareas "ready" que le tocan a este usuario (filtra por su actor/grupo)
        List<Map<String, Object>> tareasBonita = bonitaClient.tareasPendientes(usuario.bonita());

        List<TareaDTO> resultado = new ArrayList<>();

        for (Map<String, Object> tarea : tareasBonita) {

            // Bonita devuelve todo como texto -> lo convertimos a número
            Long tareaId = Long.valueOf(tarea.get("id").toString());
            String nombre = tarea.get("name").toString();
            Long caseId = Long.valueOf(tarea.get("caseId").toString());

            // 2. Postgres: la emergencia de ese case.
            //    Si no hay (ej: cases de prueba viejos), la tarea se saltea.
            emergenciaRepository.findByBonitaCaseId(caseId)
                    .ifPresent(emergencia -> resultado.add(
                            new TareaDTO(tareaId, nombre, EmergenciaDTO.desde(emergencia))
                    ));
        }

        return resultado;
    }
}