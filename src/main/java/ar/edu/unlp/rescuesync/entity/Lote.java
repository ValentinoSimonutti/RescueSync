package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

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
    private List<Oferta> ofertas;
}
