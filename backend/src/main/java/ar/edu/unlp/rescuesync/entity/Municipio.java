package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Municipio extends Organizacion {
    // es el que va a publicar las emergencias

    @OneToMany(mappedBy = "municipio")
    private List<Emergencia> emergencias = new ArrayList<>();

    protected Municipio() {
    }

    public Municipio(String nombre) {
        super(nombre);
    }

    public void cargarEmergencia(Emergencia emergencia) {
        this.emergencias.add(emergencia);
        emergencia.setMunicipio(this);
    }

    public List<Emergencia> getEmergencias() {
        return emergencias;
    }
}