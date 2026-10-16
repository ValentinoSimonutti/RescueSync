package ar.edu.unlp.sistemanacional.entity.enums;

// Resultado de la validación por niveles: en lugar de aceptar/rechazar, dice en qué rol
// puede participar la ONG con ese recurso en la emergencia.
public enum RolAsignado {
    PRINCIPAL,                  // puede ser responsable del lote
    APOYO_SECUNDARIO,           // puede participar, pero como apoyo de un responsable principal
    SIN_HABILITACION_VIGENTE    // hoy no puede participar con ese recurso (se explica en observaciones)
}
