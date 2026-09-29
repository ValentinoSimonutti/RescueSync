package ar.edu.unlp.rescuesync.auth;

import ar.edu.unlp.rescuesync.bonita.BonitaSession;

import java.io.Serializable;

// Lo que Spring guarda en la sesión (en el servidor) cuando alguien se loguea.
// Junta los dos mundos: quién es en Postgres (rol, organización) + su sesión de Bonita.
// Esto NUNCA se manda entero al front (la BonitaSession no sale del servidor).
public record UsuarioLogueado(
        Long id,                    // id del Usuario en Postgres
        String username,            // ej: "municipio1" (mismo nombre que en Bonita)
        String nombre,
        Rol rol,
        Long organizacionId,        // municipio / centro / ONG al que pertenece (null si es auditor)
        String organizacionNombre,
        BonitaSession bonita        // credencial para hablar con Bonita en su nombre
) implements Serializable {
}
