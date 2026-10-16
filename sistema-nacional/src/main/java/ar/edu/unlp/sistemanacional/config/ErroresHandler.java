package ar.edu.unlp.sistemanacional.config;

import ar.edu.unlp.sistemanacional.service.ConflictoRecursosException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// Todos los errores salen como ProblemDetail (RFC 7807): {status, title, detail, instance, ...}.
// Las ResponseStatusException de los services ya los maneja la clase base.
@RestControllerAdvice
public class ErroresHandler extends ResponseEntityExceptionHandler {

    // 400 con el detalle de cada campo inválido: "errores": {"cuit": "no debe estar vacío"}
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    // Por si dos requests chocan contra una restricción unique (ej: mismo CUIT al mismo tiempo)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operación choca con datos existentes (registro duplicado)");
    }

    // Compromiso rechazado (todo o nada): detalle de cada aporte en "items"
    @ExceptionHandler(ConflictoRecursosException.class)
    public ProblemDetail conflictoRecursos(ConflictoRecursosException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setProperty("items", ex.getItems());
        return problema;
    }

    // Lock no obtenido / deadlock / versión optimista: otra operación tocó los mismos recursos
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail concurrencia(ConcurrencyFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Los recursos están siendo modificados por otra operación, reintentar");
    }
}
