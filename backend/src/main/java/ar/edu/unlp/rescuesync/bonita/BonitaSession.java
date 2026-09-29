package ar.edu.unlp.rescuesync.bonita;

import org.springframework.http.HttpHeaders;

public record BonitaSession(
        String jsessionId,
        String apiToken,
        Long userId
) {

    public void aplicar(HttpHeaders headers) {
        headers.add(
                HttpHeaders.COOKIE,
                "JSESSIONID=" + jsessionId + "; X-Bonita-API-Token=" + apiToken
        );

        headers.add(
                "X-Bonita-API-Token",
                apiToken
        );
    }
}