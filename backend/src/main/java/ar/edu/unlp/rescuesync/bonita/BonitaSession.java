package ar.edu.unlp.rescuesync.bonita;

import org.springframework.http.HttpHeaders;

import java.io.Serializable;

// Representa la sesión de UN usuario en Bonita (lo que en Postman eran las cookies).
// Se crea en el login y se pasa a cada método del BonitaClient.
// Serializable: porque en el Paso 6 se guarda dentro de la HttpSession de Spring.
public record BonitaSession(
        String jsessionId,   // cookie de sesión: le dice a Bonita "soy este usuario"
        String apiToken,     // token anti-CSRF: Bonita lo exige en POST/PUT/DELETE
        Long userId,         // id del usuario en Bonita (para pedir sus tareas)
        String username      // ej: "municipio1" -> para buscar el Usuario en Postgres
) implements Serializable {

    // Agrega a una request lo mismo que Postman mandaba: cookies + header del token.
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
