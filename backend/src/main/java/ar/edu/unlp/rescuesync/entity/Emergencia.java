package ar.edu.unlp.rescuesync.entity;

import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.Gravedad;
import ar.edu.unlp.rescuesync.entity.enums.TipoDesastre;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Emergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String zonaAfectada;
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoDesastre tipoDesastre;

    @Enumerated(EnumType.STRING)
    private Gravedad gravedad;

    private Long bonitaCaseId;

    private LocalDateTime fechaCierreOfertas;

    @Enumerated(EnumType.STRING)
    private EstadoEmergencia estado;

    // Por qué el Sistema Nacional rechazó el último compromiso (se muestra al volver a adjudicar)
    @Column(length = 2000)
    private String avisoCompromiso;

    // El coordinador decidió seguir con los lotes que sí tienen oferta validada (el resto queda sin cubrir)
    private Boolean coberturaParcial;

    @ManyToOne
    private Municipio municipio;

    @OneToMany(mappedBy="emergencia")
    private List<Lote> lotes = new java.util.ArrayList<>();

    public Emergencia(){
    }

    public Emergencia(String zonaAfectada, String descripcion, TipoDesastre tipoDesastre, Gravedad gravedad, Municipio municipio) {
        this.zonaAfectada = zonaAfectada;
        this.descripcion = descripcion;
        this.tipoDesastre = tipoDesastre;
        this.gravedad = gravedad;
        this.municipio = municipio;
        this.estado = EstadoEmergencia.REGISTRADA;
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

    public LocalDateTime getFechaCierreOfertas() {
        return fechaCierreOfertas;
    }

    public void setFechaCierreOfertas(LocalDateTime fechaCierreOfertas) {
        this.fechaCierreOfertas = fechaCierreOfertas;
    }

    public EstadoEmergencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoEmergencia estado) {
        this.estado = estado;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Long getId() {
        return id;
    }

    public boolean isCoberturaParcial() {
        return coberturaParcial != null && coberturaParcial;
    }

    public void setCoberturaParcial(boolean coberturaParcial) {
        this.coberturaParcial = coberturaParcial;
    }

    public String getAvisoCompromiso() {
        return avisoCompromiso;
    }

    public void setAvisoCompromiso(String avisoCompromiso) {
        this.avisoCompromiso = avisoCompromiso;
    }

    public void addLote(Lote lote) {
        this.lotes.add(lote);
        lote.setEmergencia(this);
    }
}
