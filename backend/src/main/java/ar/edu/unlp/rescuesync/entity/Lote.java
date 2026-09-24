package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Lote {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String recurso;

    private int cantidad;

    @ManyToOne
    private Emergencia emergencia;

    @OneToMany(mappedBy="lote")
    private List<Oferta> ofertas= new ArrayList<Oferta>();

    public Lote(){
    }

    public Lote(String recurso, int cantidad, Emergencia emergencia) {
        this.recurso = recurso;
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
