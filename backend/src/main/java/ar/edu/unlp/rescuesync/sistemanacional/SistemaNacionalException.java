package ar.edu.unlp.rescuesync.sistemanacional;

// Error propio para cualquier problema al hablar con la API del Sistema Nacional
// (igual que BonitaException: el resto del backend no ve las excepciones de RestClient).
public class SistemaNacionalException extends RuntimeException {

    // Código HTTP que devolvió la API (401, 409...). 0 si no hubo respuesta (ej: API caída o dormida).
    private final int status;

    public SistemaNacionalException(String mensaje) {
        this(mensaje, 0, null);
    }

    public SistemaNacionalException(String mensaje, int status, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
