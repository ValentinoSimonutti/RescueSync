package ar.edu.unlp.rescuesync.bonita;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Única clase del backend que habla HTTP con Bonita.
// Cada método = una request que probamos en Postman.
// No guarda sesiones: cada método recibe la BonitaSession del usuario que hace la acción.
@Component
public class BonitaClient {

    // Le dice a Jackson que la respuesta es una LISTA de objetos JSON (no un objeto solo)
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_JSON =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;
    private final String baseUrl;
    private final String nombreProceso;

    public BonitaClient(
            @Value("${bonita.url}") String baseUrl,
            @Value("${bonita.process-name}") String nombreProceso) {   // CAMBIO: sale del yaml

        this.baseUrl = baseUrl;
        this.nombreProceso = nombreProceso;
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // ---- Postman 1: login ------------------------------------------------------
    public BonitaSession login(String user, String pass) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", user);
        form.add("password", pass);
        form.add("redirect", "false");

        List<String> cookies = llamar("login de " + user, () -> http.post()
                .uri("/loginservice")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                    // CAMBIO: si Bonita rechaza el login (401) lo decimos claro
                    if (response.getStatusCode().isError()) {
                        throw new BonitaException(
                                "Usuario o contraseña inválidos en Bonita",
                                response.getStatusCode().value(),
                                null
                        );
                    }
                    return response.getHeaders().get(HttpHeaders.SET_COOKIE);
                }));

        String jsessionId = extraerCookie(cookies, "JSESSIONID");
        String apiToken = extraerCookie(cookies, "X-Bonita-API-Token");

        BonitaSession sesionParcial = new BonitaSession(jsessionId, apiToken, null, user);

        // Con la sesión armada preguntamos "¿quién soy?" para obtener el user_id
        Map<?, ?> datosSesion = llamar("obtener datos de la sesión", () -> http.get()
                .uri("/API/system/session/unusedid")
                .headers(sesionParcial::aplicar)
                .retrieve()
                .body(Map.class));

        if (datosSesion == null || datosSesion.get("user_id") == null) {
            throw new BonitaException("Bonita no devolvió el user_id");
        }

        Long userId = Long.parseLong(datosSesion.get("user_id").toString());

        return new BonitaSession(jsessionId, apiToken, userId, user);
    }

    // ---- Postman 2: buscar el proceso -----------------------------------------
    // CAMBIO: ya no se guarda en memoria. Cada deploy nuevo cambia el id,
    // y si lo guardábamos Spring seguía usando uno viejo (404 hasta reiniciar).
    public long getProcessId(BonitaSession sesion) {

        List<Map<String, Object>> procesos = llamar("buscar el proceso", () -> http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/process")
                        .queryParam("p", 0)
                        .queryParam("c", 10)
                        .queryParam("f", "name=" + nombreProceso)
                        .queryParam("f", "activationState=ENABLED")   // CAMBIO: solo versiones habilitadas
                        .build())
                .headers(sesion::aplicar)
                .retrieve()
                .body(LISTA_JSON));

        if (procesos == null || procesos.isEmpty()) {
            throw new BonitaException(
                    "No hay un proceso '" + nombreProceso + "' deployado y habilitado en Bonita"
            );
        }

        return Long.parseLong(procesos.get(0).get("id").toString());
    }

    // ---- Postman 3: crear un case ------------------------------------------------
    public long instanciar(BonitaSession sesion) {

        long processId = getProcessId(sesion);

        Map<?, ?> respuesta = llamar("instanciar el proceso", () -> http.post()
                .uri("/API/bpm/process/{id}/instantiation", processId)
                .headers(sesion::aplicar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())        // contract del proceso vacío -> se manda {}
                .retrieve()
                .body(Map.class));

        if (respuesta == null || respuesta.get("caseId") == null) {
            throw new BonitaException("Bonita no devolvió el caseId");
        }

        return Long.parseLong(respuesta.get("caseId").toString());
    }

    // ---- Postman 4: la tarea lista de un case (con reintento) -----------------
    // Reintenta porque Bonita mueve el case de forma asíncrona:
    // justo después de instanciar, la tarea puede tardar unos ms en aparecer.
    public long buscarTareaLista(BonitaSession sesion, long caseId) {

        for (int intento = 1; intento <= 5; intento++) {

            List<Map<String, Object>> tareas = llamar("buscar tareas del case " + caseId, () -> http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/API/bpm/humanTask")
                            .queryParam("p", 0)
                            .queryParam("c", 10)
                            .queryParam("f", "caseId=" + caseId)
                            .queryParam("f", "state=ready")
                            .build())
                    .headers(sesion::aplicar)
                    .retrieve()
                    .body(LISTA_JSON));

            if (tareas != null && !tareas.isEmpty()) {

                Object id = tareas.get(0).get("id");

                if (id != null) {
                    return Long.parseLong(id.toString());
                }
            }

            if (intento < 5) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BonitaException("Se interrumpió la espera de la tarea de Bonita", 0, e);
                }
            }
        }

        throw new BonitaException("No apareció una tarea lista para el case " + caseId);
    }

    // ---- Bandeja: tareas que puede hacer el usuario logueado -----------------
    public List<Map<String, Object>> tareasPendientes(BonitaSession sesion) {

        List<Map<String, Object>> tareas = llamar("buscar tareas pendientes", () -> http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/humanTask")
                        .queryParam("p", 0)
                        .queryParam("c", 50)
                        .queryParam("f", "state=ready")
                        .queryParam("f", "user_id=" + sesion.userId())
                        .build())
                .headers(sesion::aplicar)
                .retrieve()
                .body(LISTA_JSON));

        return tareas == null ? List.of() : tareas;
    }

    // ---- Postman 5 y 7: completar una tarea mandando su contract --------------
    public void ejecutarTarea(
            BonitaSession sesion,
            long taskId,
            Map<String, Object> contrato) {

        llamar("ejecutar la tarea " + taskId, () -> http.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/userTask/{id}/execution")
                        .queryParam("assign", "true")   // la asigna al usuario y la ejecuta en un paso
                        .build(taskId))
                .headers(sesion::aplicar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(contrato)
                .retrieve()
                .toBodilessEntity());
    }

    // ---- helpers -----------------------------------------------------------------

    // NUEVO: envuelve cada llamada. Si Bonita responde error (400, 403...) o no responde,
    // tira una BonitaException con un mensaje claro (incluye lo que respondió Bonita).
    private <T> T llamar(String accion, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (BonitaException e) {
            throw e;                                    // ya viene armada (ej: login inválido)
        } catch (RestClientResponseException e) {
            throw new BonitaException(
                    "Error al " + accion + " -> HTTP " + e.getStatusCode().value()
                            + ": " + e.getResponseBodyAsString(),
                    e.getStatusCode().value(),
                    e
            );
        } catch (ResourceAccessException e) {
            throw new BonitaException(
                    "No se pudo conectar con Bonita en " + baseUrl + " (¿está abierto el Studio?)",
                    0,
                    e
            );
        }
    }

    // Saca el valor de una cookie del header Set-Cookie. Ej: "JSESSIONID=ABC; Path=/" -> "ABC"
    private String extraerCookie(List<String> cookies, String nombre) {

        if (cookies == null) {
            throw new BonitaException("Bonita no devolvió cookies de sesión");
        }

        String prefijo = nombre + "=";

        return cookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])   // CAMBIO: no falla si la cookie no tiene ';'
                .filter(par -> par.startsWith(prefijo))
                .map(par -> par.substring(prefijo.length()))
                .findFirst()
                .orElseThrow(() -> new BonitaException("Bonita no devolvió la cookie " + nombre));
    }
}
