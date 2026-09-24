package ar.edu.unlp.rescuesync.auth;

import ar.edu.unlp.rescuesync.entity.CentroCoordinador;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.entity.ONG;
import ar.edu.unlp.rescuesync.entity.Organizacion;

public enum Rol {

    // Cada rol se asocia con el tipo de organización al que debe pertenecer el usuario.
    OPERADOR_MUNICIPAL(Municipio.class),
    CCR(CentroCoordinador.class),
    REPRESENTANTE_ONG(ONG.class),
    AUDITOR(null);

    private final Class<? extends Organizacion> tipoOrganizacion;

    Rol(Class<? extends Organizacion> tipoOrganizacion) {
        this.tipoOrganizacion = tipoOrganizacion;
    }

    public Class<? extends Organizacion> getTipoOrganizacion() {
        return tipoOrganizacion;
    }
}