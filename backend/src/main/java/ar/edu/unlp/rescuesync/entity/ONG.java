package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ONG extends Organizacion {
    // es el que va a publicar las ofertas

    @OneToMany(mappedBy = "ong")
    private List<Oferta> ofertas = new ArrayList<>();

    protected ONG() {
    }

    public ONG(String nombre) {
        super(nombre);
    }

    public List<Oferta> getOfertas() {
        return ofertas;
    }
}