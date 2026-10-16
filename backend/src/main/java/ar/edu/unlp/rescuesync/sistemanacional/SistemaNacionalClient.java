package ar.edu.unlp.rescuesync.sistemanacional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

// Única clase del backend que habla HTTP con la API del Sistema Nacional.
// Usa siempre la cuenta de sistema (la misma que Bonita) y guarda el JWT hasta que esté por vencer.
@Component
public class SistemaNacionalClient {

    private static final ParameterizedTypeReference<List<TipoRecursoDTO>> LISTA_TIPOS =
            new ParameterizedTypeReference<>() {};

    // El catálogo casi no cambia: se pide de nuevo cada 10 minutos
    private static final Duration DURACION_CATALOGO = Duration.ofMinutes(10);

    private final RestClient http;
    private final String baseUrl;
    private final String usuario;
    private final String password;

    private String token;
    private Instant vencimientoToken = Instant.EPOCH;

    private List<TipoRecursoDTO> catalogo;
    private Instant vencimientoCatalogo = Instant.EPOCH;

    public SistemaNacionalClient(
            @Value("${sistema-nacional.url}") String baseUrl,
            @Value("${sistema-nacional.usuario}") String usuario,
            @Value("${sistema-nacional.password}") String password) {

        this.baseUrl = baseUrl;
        this.usuario = usuario;
        this.password = password;

        // Render duerme la API a los 15 min sin uso y tarda ~1 min en despertar:
        // por eso el timeout de lectura es largo.
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(10));
        timeouts.setReadTimeout(Duration.ofSeconds(90));

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeouts)
                .build();
    }

    // ---- Catálogo de tipos de recurso -------------------------------------------
    // Si la API no responde pero ya habíamos traído el catálogo, se sigue usando el anterior.
    public synchronized List<TipoRecursoDTO> tiposRecurso() {

        if (catalogo != null && Instant.now().isBefore(vencimientoCatalogo)) {
            return catalogo;
        }

        try {
            List<TipoRecursoDTO> tipos = conToken("traer el catálogo de recursos", jwt -> http.get()
                    .uri("/api/catalogo/tipos-recurso")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(LISTA_TIPOS));

            catalogo = tipos == null ? List.of() : tipos;
            vencimientoCatalogo = Instant.now().plus(DURACION_CATALOGO);
            return catalogo;

        } catch (SistemaNacionalException e) {
            if (catalogo != null) {
                return catalogo;
            }
            throw e;
        }
    }

    public Optional<TipoRecursoDTO> tipoRecurso(String codigo) {
        return tiposRecurso().stream()
                .filter(tipo -> tipo.codigo().equals(codigo))
                .findFirst();
    }

    // ---- helpers -----------------------------------------------------------------

    // Hace la llamada con el token guardado. Si la API responde 401 (token vencido o
    // la API se reinició con otro JWT_SECRET), hace login de nuevo y reintenta una vez.
    private synchronized <T> T conToken(String accion, Function<String, T> llamada) {
        try {
            return llamar(accion, () -> llamada.apply(tokenVigente()));
        } catch (SistemaNacionalException e) {
            if (e.getStatus() != 401) {
                throw e;
            }
            vencimientoToken = Instant.EPOCH;
            return llamar(accion, () -> llamada.apply(tokenVigente()));
        }
    }

    private String tokenVigente() {

        if (token != null && Instant.now().isBefore(vencimientoToken)) {
            return token;
        }

        Map<?, ?> respuesta = llamar("iniciar sesión como " + usuario, () -> http.post()
                .uri("/api/auth/login")
                .body(Map.of("username", usuario, "password", password))
                .retrieve()
                .body(Map.class));

        if (respuesta == null || respuesta.get("token") == null) {
            throw new SistemaNacionalException("El Sistema Nacional no devolvió el token");
        }

        long segundos = respuesta.get("expiraEnSegundos") instanceof Number n ? n.longValue() : 3600;

        token = respuesta.get("token").toString();
        vencimientoToken = Instant.now().plusSeconds(segundos).minusSeconds(60);   // margen de 1 minuto
        return token;
    }

    // Envuelve cada llamada y traduce los errores de RestClient a un mensaje claro
    private <T> T llamar(String accion, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (SistemaNacionalException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new SistemaNacionalException(
                    "Error al " + accion + " en el Sistema Nacional -> HTTP " + e.getStatusCode().value()
                            + ": " + e.getResponseBodyAsString(),
                    e.getStatusCode().value(),
                    e
            );
        } catch (ResourceAccessException e) {
            throw new SistemaNacionalException(
                    "No se pudo conectar con el Sistema Nacional en " + baseUrl
                            + " (si está en Render, puede estar despertando: probá de nuevo en un minuto)",
                    0,
                    e
            );
        }
    }
}
