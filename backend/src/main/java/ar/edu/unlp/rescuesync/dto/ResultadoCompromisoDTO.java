package ar.edu.unlp.rescuesync.dto;

// Lo que le devolvemos a Bonita después de "Comprometer Recursos".
// compromisoOk decide la compuerta: true -> Monitorear, false -> volver a "Adjudicar y notificar".
public record ResultadoCompromisoDTO(Long emergenciaId, boolean compromisoOk, String aviso) {
}
