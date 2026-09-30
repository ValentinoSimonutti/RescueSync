package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.enums.Gravedad;
import ar.edu.unlp.rescuesync.entity.enums.TipoDesastre;

public class NuevaEmergenciaDTO {

    private String zonaAfectada;
    private String descripcion;
    private TipoDesastre tipoDesastre;
    private Gravedad gravedad;

    public NuevaEmergenciaDTO() {
    }

    public String getZonaAfectada() {
        return zonaAfectada;
    }

    public void setZonaAfectada(String zonaAfectada) {
        this.zonaAfectada = zonaAfectada;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoDesastre getTipoDesastre() {
        return tipoDesastre;
    }

    public void setTipoDesastre(TipoDesastre tipoDesastre) {
        this.tipoDesastre = tipoDesastre;
    }

    public Gravedad getGravedad() {
        return gravedad;
    }

    public void setGravedad(Gravedad gravedad) {
        this.gravedad = gravedad;
    }
}