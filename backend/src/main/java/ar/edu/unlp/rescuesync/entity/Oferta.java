package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Oferta {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private int cantidad;
    private String recurso;

    @ManyToOne
    private ONG ong;

    @ManyToOne
    private Lote lote;

    public Oferta(){
    }

    public Oferta(int cantidad, String recurso, ONG ong, Lote lote) {
        this.cantidad = cantidad;
        this.recurso = recurso;
        this.ong = ong;
        this.lote = lote;
    }

    public Long getId() {
        return id;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public ONG getOng() {
        return ong;
    }

    public void setOng(ONG ong) {
        this.ong = ong;
    }

    public Lote getLote() {
        return lote;
    }

    public void setLote(Lote lote) {
        this.lote = lote;
    }
}
