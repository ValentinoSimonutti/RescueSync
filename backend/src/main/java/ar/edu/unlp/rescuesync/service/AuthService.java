/*
package ar.edu.unlp.rescuesync.service;

import ar.edu.unlp.rescuesync.auth.Usuario;
import ar.edu.unlp.rescuesync.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario login(String email, String password) {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario o contraseña incorrectos"
                        )
                );

        if (!usuario.getPassword().equals(password)) {
            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos"
            );
        }

        return usuario;
    }
}
 */
