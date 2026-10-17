package ar.edu.unlp.rescuesync.entity.enums;

// Perfil que asigna el Sistema Nacional al validar una oferta (o el aporte de una ONG)
public enum RolValidacion {
    PRINCIPAL,                  // puede ser responsable del lote
    APOYO_SECUNDARIO,           // participa como apoyo (certificación vencida o de nivel bajo)
    SIN_HABILITACION_VIGENTE    // no puede participar
}
