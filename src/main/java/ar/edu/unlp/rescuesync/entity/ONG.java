package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ONG {

    // es el que va a publicar las ofertas

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @OneToMany
    private List<Oferta> ofertas= new ArrayList<Oferta>();

    public ONG(){

    }
}
