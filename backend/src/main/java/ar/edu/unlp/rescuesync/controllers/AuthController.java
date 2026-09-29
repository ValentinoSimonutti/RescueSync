/*
package ar.edu.unlp.rescuesync.controllers;

import ar.edu.unlp.rescuesync.auth.Usuario;
import ar.edu.unlp.rescuesync.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Usuario login(
            @RequestParam String email,
            @RequestParam String password) {

        return authService.login(email, password);
    }
}
 */