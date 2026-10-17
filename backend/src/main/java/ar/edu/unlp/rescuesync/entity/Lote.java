package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Lote {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String recurso;          // nombre para mostrar (ej: "Paramédicos")

    private String recursoCodigo;    // código del catálogo nacional (ej: "PARAMEDICO")

    private int cantidad;

    // false = lote de una convocatoria anterior (el coordinador la reabrió con lotes nuevos).
    // null en filas viejas = vigente.
    private Boolean vigente = true;

    @ManyToOne
    private Emergencia emergencia;

    @OneToMany(mappedBy="lote")
    private List<Oferta> ofertas= new ArrayList<Oferta>();

    public Lote(){
    }

    public Lote(String recurso, String recursoCodigo, int cantidad, Emergencia emergencia) {
        this.recurso = recurso;
        this.recursoCodigo = recursoCodigo;
        this.cantidad = cantidad;
        this.emergencia = emergencia;
    }

    public Long getId() {
        return id;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public String getRecursoCodigo() {
        return recursoCodigo;
    }

    public void setRecursoCodigo(String recursoCodigo) {
        this.recursoCodigo = recursoCodigo;
    }

    public boolean isVigente() {
        return vigente == null || vigente;
    }

    public void archivar() {
        this.vigente = false;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Emergencia getEmergencia() {
        return emergencia;
    }

    public void setEmergencia(Emergencia emergencia) {
        this.emergencia = emergencia;
    }

    public List<Oferta> getOfertas() {
        return ofertas;
    }

    public void setOfertas(List<Oferta> ofertas) {
        this.ofertas = ofertas;
    }
}
