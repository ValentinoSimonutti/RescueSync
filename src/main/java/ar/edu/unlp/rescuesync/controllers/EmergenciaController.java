package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.service.RescueSyncService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergencias")
public class EmergenciaController{

    private RescueSyncService service;

    public EmergenciaController(RescueSyncService service){
        this.service=service;
    }

    public Emergencia cargar(@RequestBody Emergencia emergencia){
        return service.cargarEmergencia(emergencia);
    }
}
