package ar.edu.unlp.rescuesync.entity;

import ar.edu.unlp.rescuesync.entity.enums.Gravedad;
import ar.edu.unlp.rescuesync.entity.enums.TipoDesastre;
import jakarta.persistence.*;

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

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Long getId() {
        return id;
    }

    public void addLote(Lote lote) {
        this.lotes.add(lote);
    }
}
