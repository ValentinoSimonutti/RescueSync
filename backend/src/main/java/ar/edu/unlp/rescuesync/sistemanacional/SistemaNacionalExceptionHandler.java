package ar.edu.unlp.rescuesync.sistemanacional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Convierte una SistemaNacionalException en un JSON claro para el front.
// Siempre 502 (Bad Gateway): falló un sistema externo, no el pedido del usuario.
@RestControllerAdvice
public class SistemaNacionalExceptionHandler {

    @ExceptionHandler(SistemaNacionalException.class)
    public ResponseEntity<Map<String, Object>> manejar(SistemaNacionalException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "Sistema Nacional",
                "mensaje", e.getMessage(),
                "statusSistemaNacional", e.getStatus()
        ));
    }
}
