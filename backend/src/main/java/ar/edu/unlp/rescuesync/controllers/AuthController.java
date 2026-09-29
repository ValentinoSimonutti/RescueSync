package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.AuthService;
import ar.edu.unlp.rescuesync.auth.SesionActual;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;
import ar.edu.unlp.rescuesync.dto.LoginRequest;
import ar.edu.unlp.rescuesync.dto.UsuarioDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SesionActual sesionActual;

    public AuthController(AuthService authService, SesionActual sesionActual) {
        this.authService = authService;
        this.sesionActual = sesionActual;
    }

    // POST /api/auth/login  body: {"username":"municipio1","password":"bpm"}
    @PostMapping("/login")
    public UsuarioDTO login(@RequestBody LoginRequest datos, HttpServletRequest request) {

        UsuarioLogueado usuario = authService.login(datos.username(), datos.password());

        // Si había una sesión vieja la descartamos y creamos una nueva
        // (evita que alguien reutilice un id de sesión anterior: "session fixation").
        HttpSession vieja = request.getSession(false);
        if (vieja != null) {
            vieja.invalidate();
        }
        HttpSession nueva = request.getSession(true);   // acá Spring genera la cookie JSESSIONID

        sesionActual.guardar(nueva, usuario);

        return UsuarioDTO.from(usuario);
    }

    // GET /api/auth/me  -> quién está logueado (401 si nadie)
    @GetMapping("/me")
    public UsuarioDTO me(HttpSession session) {
        return UsuarioDTO.from(sesionActual.obtener(session));
    }

    // POST /api/auth/logout  -> borra la sesión de Spring
    @PostMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
