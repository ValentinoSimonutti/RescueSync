package ar.edu.unlp.sistemanacional.security;

import ar.edu.unlp.sistemanacional.entity.Usuario;
import ar.edu.unlp.sistemanacional.entity.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// Firma (HS256) y valida los tokens. El token lleva usuario, rol y CUIT de la ONG,
// así cada request se autoriza sin consultar la base.
@Service
public class JwtService {

    private static final String EMISOR = "sistema-nacional";

    private final SecretKey clave;
    private final Duration expiracion;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiracion-minutos}") long expiracionMinutos) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) tiene que tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("cuit", usuario.getOng() == null ? null : usuario.getOng().getCuit())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    // Tira JwtException si la firma no coincide, está vencido o mal formado
    public UsuarioAutenticado validar(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new UsuarioAutenticado(
                claims.getSubject(),
                Rol.valueOf(claims.get("rol", String.class)),
                claims.get("cuit", String.class)
        );
    }

    public long getExpiracionSegundos() {
        return expiracion.toSeconds();
    }
}
