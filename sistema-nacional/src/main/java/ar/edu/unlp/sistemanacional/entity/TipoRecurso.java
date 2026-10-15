package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.CategoriaRecurso;
import jakarta.persistence.*;

// Tipo de recurso del catálogo nacional (ej: PARAMEDICO, RACION_ALIMENTO).
// La app local usa el código en sus lotes, así ambos sistemas hablan del mismo recurso.
@Entity
public class TipoRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private CategoriaRecurso categoria;

    private String unidad;   // ej: "persona", "ración", "unidad"

    // Certificación que hace falta para ofrecer este recurso como responsable principal.
    // null = no requiere certificación (ej: carpas, agua).
    @ManyToOne
    private Certificacion certificacionRequerida;

    protected TipoRecurso() {
    }

    public TipoRecurso(String codigo, String nombre, CategoriaRecurso categoria, String unidad,
                       Certificacion certificacionRequerida) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidad = unidad;
        this.certificacionRequerida = certificacionRequerida;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public CategoriaRecurso getCategoria() { return categoria; }
    public String getUnidad() { return unidad; }
    public Certificacion getCertificacionRequerida() { return certificacionRequerida; }
}
