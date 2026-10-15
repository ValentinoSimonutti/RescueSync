package ar.edu.unlp.sistemanacional.entity;

import jakarta.persistence.*;

// Certificación del catálogo nacional (ej: "Rescate acuático").
// Los tipos de recurso que habilita se ven desde TipoRecurso.certificacionRequerida.
@Entity
public class Certificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;

    @Column(length = 500)
    private String descripcion;

    protected Certificacion() {
    }

    public Certificacion(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
