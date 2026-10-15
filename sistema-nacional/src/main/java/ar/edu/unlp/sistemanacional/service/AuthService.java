package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.LoginRequest;
import ar.edu.unlp.sistemanacional.dto.LoginResponse;
import ar.edu.unlp.sistemanacional.entity.Usuario;
import ar.edu.unlp.sistemanacional.repository.UsuarioRepository;
import ar.edu.unlp.sistemanacional.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Chequea usuario + contraseña (BCrypt) contra la tabla Usuario
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Mismo mensaje si no existe o si la contraseña está mal (no revela qué usuarios existen)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElseThrow();

        return new LoginResponse(
                jwtService.generar(usuario),
                "Bearer",
                jwtService.getExpiracionSegundos(),
                usuario.getUsername(),
                usuario.getRol().name(),
                usuario.getOng() == null ? null : usuario.getOng().getCuit()
        );
    }
}
