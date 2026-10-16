package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ONG extends Organizacion {
    // es el que va a publicar las ofertas

    // Vincula la ONG local con la del Sistema Nacional (11 dígitos, sin guiones)
    @Column(unique = true, length = 11)
    private String cuit;

    @OneToMany(mappedBy = "ong")
    private List<Oferta> ofertas = new ArrayList<>();

    protected ONG() {
    }

    public ONG(String nombre, String cuit) {
        super(nombre);
        this.cuit = cuit;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public List<Oferta> getOfertas() {
        return ofertas;
    }
}