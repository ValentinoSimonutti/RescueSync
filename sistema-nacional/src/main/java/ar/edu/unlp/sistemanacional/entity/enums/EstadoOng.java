package ar.edu.unlp.sistemanacional.entity.enums;

public enum EstadoOng {
    PENDIENTE,    // se registró (onboarding) y espera que un ADMIN la revise
    HABILITADA,   // puede ofrecer recursos y recibir compromisos
    SUSPENDIDA    // el ADMIN la dio de baja temporalmente
}
