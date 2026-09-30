package ar.edu.unlp.rescuesync.entity.enums;

// Estado del aporte de UNA ONG dentro de una oferta
public enum EstadoAporte {
    PENDIENTE,    // la invitaron a un consorcio y todavía no respondió (no suma)
    CONFIRMADO,   // aporte vigente (suma a la oferta)
    RECHAZADO,    // rechazó la invitación al consorcio
    RETIRADO      // estaba confirmado y se retiró
}
