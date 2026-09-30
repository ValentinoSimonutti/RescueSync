package ar.edu.unlp.rescuesync.dto;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.Gravedad;
import ar.edu.unlp.rescuesync.entity.enums.TipoDesastre;

import java.time.LocalDateTime;

public class EmergenciaDTO {

    private Long id;
    private String zonaAfectada;
    private String descripcion;
    private TipoDesastre tipoDesastre;
    private Gravedad gravedad;
    private Long bonitaCaseId;
    private EstadoEmergencia estado;
    private LocalDateTime fechaCierreOfertas;

    public EmergenciaDTO() {
    }

    // Convierte la entidad (lo que está en la base) en el DTO (lo que viaja al front).
    // static: se llama sin crear un objeto antes -> EmergenciaDTO.desde(emergencia)
    public static EmergenciaDTO desde(Emergencia emergencia) {
        EmergenciaDTO dto = new EmergenciaDTO();
        dto.setId(emergencia.getId());
        dto.setZonaAfectada(emergencia.getZonaAfectada());
        dto.setDescripcion(emergencia.getDescripcion());
        dto.setTipoDesastre(emergencia.getTipoDesastre());
        dto.setGravedad(emergencia.getGravedad());
        dto.setBonitaCaseId(emergencia.getBonitaCaseId());
        dto.setEstado(emergencia.getEstado());
        dto.setFechaCierreOfertas(emergencia.getFechaCierreOfertas());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getBonitaCaseId() {
        return bonitaCaseId;
    }

    public void setBonitaCaseId(Long bonitaCaseId) {
        this.bonitaCaseId = bonitaCaseId;
    }

    public EstadoEmergencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoEmergencia estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCierreOfertas() {
        return fechaCierreOfertas;
    }

    public void setFechaCierreOfertas(LocalDateTime fechaCierreOfertas) {
        this.fechaCierreOfertas = fechaCierreOfertas;
    }
}