package ar.edu.unlp.sistemanacional.dto;

import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;

import java.util.List;

// Resultado de validar el aporte de una ONG: no es sí/no, es un perfil.
//  - nivelCompetencia / nivelRiesgoMaximo: hasta qué gravedad puede ser responsable con ese recurso
//  - capacidadesHabilitadas: todos los tipos de recurso que hoy puede ofrecer como principal
//  - cantidadValidable: lo que realmente puede aportar según su disponibilidad nacional
//  - rolAsignado + observaciones: en qué rol entra y por qué
public record PerfilCompetencia(
        String cuit,
        String nombre,
        String estadoOng,                  // PENDIENTE / HABILITADA / SUSPENDIDA / NO_REGISTRADA
        int cantidadOfrecida,
        int disponible,
        int cantidadValidable,
        String certificacionRequerida,
        NivelCompetencia nivelCompetencia,
        NivelRiesgo nivelRiesgoMaximo,
        List<String> capacidadesHabilitadas,
        RolAsignado rolAsignado,
        List<String> observaciones
) {
    public boolean participa() {
        return rolAsignado != RolAsignado.SIN_HABILITACION_VIGENTE && cantidadValidable > 0;
    }
}
