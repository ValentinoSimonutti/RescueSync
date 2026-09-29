package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev")
public class BonitaDevController {

    private final BonitaClient bonitaClient;

    public BonitaDevController(BonitaClient bonitaClient) {
        this.bonitaClient = bonitaClient;
    }

    @GetMapping("/bonita-login")
    public Map<String, Object> probarLogin() {

        BonitaSession sesion =
                bonitaClient.login("municipio1", "bpm");

        long processId =
                bonitaClient.getProcessId(sesion);

        long caseId =
                bonitaClient.instanciar(sesion);

        long taskId =
                bonitaClient.buscarTareaLista(sesion, caseId);

        bonitaClient.ejecutarTarea(
                sesion,
                taskId,
                Map.of("emergenciaIdInput", 99)
        );

        var tareasPendientes =
                bonitaClient.tareasPendientes(sesion);

        return Map.of(
                "login", "OK",
                "userId", sesion.userId(),
                "processId", processId,
                "caseId", caseId,
                "taskIdEjecutada", taskId,
                "emergenciaIdEnviado", 99
        );
    }
}