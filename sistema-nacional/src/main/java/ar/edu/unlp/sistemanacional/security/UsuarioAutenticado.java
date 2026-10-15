package ar.edu.unlp.sistemanacional.security;

import ar.edu.unlp.sistemanacional.entity.enums.Rol;

// Quién hace el request, sacado del JWT (sin ir a la base).
// Los controllers lo reciben con @AuthenticationPrincipal.
// ongCuit es null para ADMIN y SISTEMA.
public record UsuarioAutenticado(String username, Rol rol, String ongCuit) {
}
