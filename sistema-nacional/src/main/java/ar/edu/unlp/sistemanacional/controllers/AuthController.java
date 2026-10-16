package ar.edu.unlp.sistemanacional.controllers;

import ar.edu.unlp.sistemanacional.dto.LoginRequest;
import ar.edu.unlp.sistemanacional.dto.LoginResponse;
import ar.edu.unlp.sistemanacional.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements   // público: no pide token
    @Operation(summary = "Login: devuelve el JWT para usar en el resto de la API")
    @ApiResponse(responseCode = "200", description = "Token JWT y datos del usuario")
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "Error")))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
