package ar.edu.unlp.rescuesync.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Helper para guardar / leer el usuario logueado de la sesión de Spring.
// Cualquier controller que necesite saber "quién está haciendo esto" usa obtener().
@Component
public class SesionActual {

    // Nombre de la "cajita" dentro de la HttpSession donde guardamos el usuario
    private static final String CLAVE = "usuarioLogueado";

    public void guardar(HttpSession session, UsuarioLogueado usuario) {
        session.setAttribute(CLAVE, usuario);
    }

    // Devuelve el usuario logueado o responde 401 si no hay nadie logueado
    public UsuarioLogueado obtener(HttpSession session) {
        Object usuario = session.getAttribute(CLAVE);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay una sesión iniciada");
        }
        return (UsuarioLogueado) usuario;
    }
}
