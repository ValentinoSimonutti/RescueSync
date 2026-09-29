package ar.edu.unlp.rescuesync.bonita;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Atrapa cualquier BonitaException que salga de un controller
// y la convierte en una respuesta JSON clara, en vez de un 500 genérico.
@RestControllerAdvice
public class BonitaExceptionHandler {

    @ExceptionHandler(BonitaException.class)
    public ResponseEntity<Map<String, Object>> manejar(BonitaException e) {

        // 401 de Bonita = credenciales inválidas -> se lo devolvemos igual al front.
        // Cualquier otro problema con Bonita -> 502 (Bad Gateway: falló un sistema externo).
        HttpStatus status = e.getStatus() == 401
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_GATEWAY;

        return ResponseEntity.status(status).body(Map.of(
                "error", "Bonita",
                "mensaje", e.getMessage(),
                "statusBonita", e.getStatus()
        ));
    }
}
