package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.DashboardBonitaDTO;
import ar.edu.unlp.rescuesync.dto.DashboardLocalDTO;
import ar.edu.unlp.rescuesync.dto.DashboardSistemaNacionalDTO;
import ar.edu.unlp.rescuesync.service.DashboardBonitaService;
import ar.edu.unlp.rescuesync.service.DashboardLocalService;
import ar.edu.unlp.rescuesync.service.DashboardSistemaNacionalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

// Dashboard del Auditor / Directivo. Un endpoint por fuente de datos: si Bonita está cerrado
// o el Sistema Nacional está despertando en Render, solo falla ese panel.
// desde / hasta (opcionales, yyyy-MM-dd) filtran por fecha de registro de la emergencia o de inicio del caso.
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardLocalService local;
    private final DashboardBonitaService bonita;
    private final DashboardSistemaNacionalService sistemaNacional;
    private final SesionActual sesionActual;

    public DashboardController(DashboardLocalService local, DashboardBonitaService bonita,
                               DashboardSistemaNacionalService sistemaNacional, SesionActual sesionActual) {
        this.local = local;
        this.bonita = bonita;
        this.sistemaNacional = sistemaNacional;
        this.sesionActual = sesionActual;
    }

    // GET /api/dashboard/local -> base de datos de la app
    @GetMapping("/local")
    public DashboardLocalDTO local(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   HttpSession session) {
        auditor(session);
        return local.indicadores(desde, hasta);
    }

    // GET /api/dashboard/bonita -> tiempos del proceso (con la sesión de Bonita del auditor)
    @GetMapping("/bonita")
    public DashboardBonitaDTO bonita(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     HttpSession session) {
        return bonita.indicadores(auditor(session).bonita(), desde, hasta);
    }

    // GET /api/dashboard/sistema-nacional -> recursos y compromisos a nivel nacional
    @GetMapping("/sistema-nacional")
    public DashboardSistemaNacionalDTO sistemaNacional(HttpSession session) {
        auditor(session);
        return sistemaNacional.indicadores();
    }

    private UsuarioLogueado auditor(HttpSession session) {
        UsuarioLogueado usuario = sesionActual.obtener(session);
        if (usuario.rol() != Rol.AUDITOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el auditor puede ver el dashboard");
        }
        return usuario;
    }
}
