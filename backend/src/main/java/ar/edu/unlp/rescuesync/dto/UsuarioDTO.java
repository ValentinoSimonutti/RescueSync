package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.UsuarioLogueado;

// Lo que le devolvemos a Vue sobre el usuario logueado.
// A propósito NO incluye la BonitaSession: esa credencial queda solo en el servidor.
public record UsuarioDTO(
        String username,
        String nombre,
        Rol rol,
        Long organizacionId,
        String organizacionNombre
) {
    public static UsuarioDTO from(UsuarioLogueado u) {
        return new UsuarioDTO(u.username(), u.nombre(), u.rol(), u.organizacionId(), u.organizacionNombre());
    }
}
