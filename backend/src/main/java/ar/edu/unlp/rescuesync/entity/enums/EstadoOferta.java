package ar.edu.unlp.rescuesync.entity.enums;

// Estado de una oferta completa (individual o consorcio)
public enum EstadoOferta {
    ACTIVA,     // vigente: cuenta para la cobertura
    RETIRADA,   // la ONG líder la retiró: no cuenta
    DESCARTADA  // el coordinador reabrió la convocatoria desde cero: no cuenta
}
