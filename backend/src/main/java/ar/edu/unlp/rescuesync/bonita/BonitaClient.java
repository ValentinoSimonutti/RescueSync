package ar.edu.unlp.rescuesync.bonita;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.List;
import java.util.Map;

@Component
public class BonitaClient {

    private final RestClient http;
    private Long processId;

    public BonitaClient(
            @Value("${bonita.url}") String baseUrl) {

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public BonitaSession login(String user, String pass) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", user);
        form.add("password", pass);
        form.add("redirect", "false");

        List<String> cookies = http.post()
                .uri("/loginservice")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) ->
                        response.getHeaders().get("Set-Cookie")
                );

        String jsessionId = extraerCookie(cookies, "JSESSIONID");
        String apiToken = extraerCookie(cookies, "X-Bonita-API-Token");

        BonitaSession sesionParcial = new BonitaSession(
                jsessionId,
                apiToken,
                null
        );

        Map<?, ?> datosSesion = http.get()
                .uri("/API/system/session/unusedid")
                .headers(sesionParcial::aplicar)
                .retrieve()
                .body(Map.class);

        if (datosSesion == null || datosSesion.get("user_id") == null) {
            throw new IllegalStateException(
                    "Bonita no devolvió el user_id"
            );
        }

        Long userId = Long.parseLong(
                datosSesion.get("user_id").toString()
        );

        return new BonitaSession(
                jsessionId,
                apiToken,
                userId
        );
    }

    public long getProcessId(BonitaSession sesion) {

        if (processId != null) {
            return processId;
        }

        List<Map<String, Object>> procesos = http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/process")
                        .queryParam("f", "name=RescueSync")
                        .build())
                .headers(sesion::aplicar)
                .retrieve()
                .body(List.class);

        if (procesos == null || procesos.isEmpty()) {
            throw new IllegalStateException(
                    "No se encontró el proceso RescueSync en Bonita"
            );
        }

        Object id = procesos.get(0).get("id");

        if (id == null) {
            throw new IllegalStateException(
                    "Bonita devolvió el proceso sin id"
            );
        }

        processId = Long.parseLong(id.toString());

        return processId;
    }

    private String extraerCookie(List<String> cookies, String nombre) {

        if (cookies == null) {
            throw new IllegalStateException(
                    "Bonita no devolvió cookies de sesión"
            );
        }

        String prefijo = nombre + "=";

        return cookies.stream()
                .filter(cookie -> cookie.startsWith(prefijo))
                .map(cookie -> cookie.substring(
                        prefijo.length(),
                        cookie.indexOf(';')
                ))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Bonita no devolvió la cookie " + nombre
                        )
                );
    }

    public long instanciar(BonitaSession sesion) {

        Map<?, ?> respuesta = http.post()
                .uri(
                        "/API/bpm/process/{id}/instantiation",
                        getProcessId(sesion)
                )
                .headers(sesion::aplicar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(Map.class);

        if (respuesta == null || respuesta.get("caseId") == null) {
            throw new IllegalStateException(
                    "Bonita no devolvió el caseId"
            );
        }

        return Long.parseLong(
                respuesta.get("caseId").toString()
        );
    }

    public long buscarTareaLista(BonitaSession sesion, long caseId) {

        for (int intento = 1; intento <= 5; intento++) {

            List<Map<String, Object>> tareas = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/API/bpm/humanTask")
                            .queryParam("f", "caseId=" + caseId)
                            .queryParam("f", "state=ready")
                            .build())
                    .headers(sesion::aplicar)
                    .retrieve()
                    .body(List.class);

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

                    throw new IllegalStateException(
                            "Se interrumpió la espera de la tarea de Bonita",
                            e
                    );
                }
            }
        }

        throw new IllegalStateException(
                "No apareció una tarea lista para el case " + caseId
        );
    }

    public List<Map<String, Object>> tareasPendientes(BonitaSession sesion) {

        List<Map<String, Object>> tareas = http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/humanTask")
                        .queryParam("f", "state=ready")
                        .queryParam("f", "user_id=" + sesion.userId())
                        .build())
                .headers(sesion::aplicar)
                .retrieve()
                .body(List.class);

        if (tareas == null) {
            return List.of();
        }

        return tareas;
    }

    public void ejecutarTarea(
            BonitaSession sesion,
            long taskId,
            Map<String, Object> contrato) {

        http.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/userTask/{id}/execution")
                        .queryParam("assign", "true")
                        .build(taskId))
                .headers(sesion::aplicar)
                .contentType(MediaType.APPLICATION_JSON)
                .body(contrato)
                .retrieve()
                .toBodilessEntity();
    }
}