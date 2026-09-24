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

}
