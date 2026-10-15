package ar.edu.unlp.sistemanacional.dto;

// token: se manda en cada request como "Authorization: Bearer <token>"
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String username,
        String rol,
        String ongCuit
) {
}
