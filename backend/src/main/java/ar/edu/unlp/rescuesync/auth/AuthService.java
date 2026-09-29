package ar.edu.unlp.rescuesync.auth;

import ar.edu.unlp.rescuesync.bonita.BonitaClient;
import ar.edu.unlp.rescuesync.bonita.BonitaSession;
import ar.edu.unlp.rescuesync.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Hace el login combinando Bonita (valida la contraseña) y Postgres (rol y organización).
@Service
public class AuthService {

    private final BonitaClient bonitaClient;
    private final UsuarioRepository usuarioRepository;

    public AuthService(BonitaClient bonitaClient, UsuarioRepository usuarioRepository) {
        this.bonitaClient = bonitaClient;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioLogueado login(String username, String password) {

        // 1. Bonita decide si la contraseña es correcta.
        //    Si no lo es, BonitaClient tira BonitaException (401) y el handler responde 401.
        BonitaSession sesionBonita = bonitaClient.login(username, password);

        // 2. Postgres dice quién es en NUESTRO sistema (rol + organización).
        Usuario usuario = usuarioRepository.findByBonitaUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El usuario existe en Bonita pero no está registrado en RescueSync"
                ));

        // 3. Juntamos todo en un solo objeto para guardar en la sesión.
        var organizacion = usuario.getOrganizacion();   // null si es auditor

        return new UsuarioLogueado(
                usuario.getId(),
                usuario.getBonitaUsername(),
                usuario.getNombre() + " " + usuario.getApellido(),
                usuario.getRol(),
                organizacion == null ? null : organizacion.getId(),
                organizacion == null ? null : organizacion.getNombre(),
                sesionBonita
        );
    }
}
