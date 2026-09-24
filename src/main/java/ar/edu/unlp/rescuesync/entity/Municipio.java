package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Municipio {

    // es el que va a publicar las emergencias
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @OneToMany(mappedBy="municipio")
    private List<Emergencia> emergencias = new ArrayList<Emergencia>();

    public Municipio(String nombre) {
        this.nombre = nombre;
    }

    public Municipio() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void cargarEmergencia(Emergencia emergencia) {
        this.emergencias.add(emergencia);
        emergencia.setMunicipio(this);
    }

    public List<Emergencia> listarEmergencias() {
        return emergencias;
    }
}
