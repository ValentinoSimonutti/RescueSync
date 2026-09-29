package ar.edu.unlp.rescuesync.bonita;

// Error propio para cualquier problema al hablar con Bonita.
// Así el resto del backend no depende de las excepciones internas de RestClient
// y el mensaje explica qué pasó (incluye lo que respondió Bonita).
public class BonitaException extends RuntimeException {

    // Código HTTP que devolvió Bonita (401, 403, 400...). 0 si no hubo respuesta (ej: Bonita apagado).
    private final int status;

    public BonitaException(String mensaje) {
        this(mensaje, 0, null);
    }

    public BonitaException(String mensaje, int status, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
