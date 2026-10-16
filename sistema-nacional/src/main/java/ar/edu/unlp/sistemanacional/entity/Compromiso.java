package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.EstadoCompromiso;
import ar.edu.unlp.sistemanacional.entity.enums.RolAsignado;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Recursos de UNA ONG bloqueados para una oferta adjudicada en una emergencia.
// Una oferta de consorcio genera un compromiso por cada ONG que aporta.
// Unique (proyecto, ofertaRef, ong): si Bonita reintenta el mismo request no se duplica.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "oferta_ref", "ong_id"}))
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Proyecto proyecto;

    // id de la oferta en la app local
    @Column(name = "oferta_ref", nullable = false)
    private String ofertaRef;

    @ManyToOne(optional = false)
    private Ong ong;

    // Fila de inventario que se descontó (y a la que se devuelve al liberar)
    @ManyToOne(optional = false)
    private RecursoInventario inventario;

    private int cantidad;

    @Enumerated(EnumType.STRING)
    private RolAsignado rolAsignado;

    @Enumerated(EnumType.STRING)
    private EstadoCompromiso estado;

    private LocalDateTime fechaCompromiso;
    private LocalDateTime fechaLiberacion;

    protected Compromiso() {
    }

    public Compromiso(Proyecto proyecto, String ofertaRef, RecursoInventario inventario, int cantidad,
                      RolAsignado rolAsignado) {
        this.proyecto = proyecto;
        this.ofertaRef = ofertaRef;
        this.ong = inventario.getOng();
        this.inventario = inventario;
        this.cantidad = cantidad;
        this.rolAsignado = rolAsignado;
        this.estado = EstadoCompromiso.ACTIVO;
        this.fechaCompromiso = LocalDateTime.now();
        proyecto.getCompromisos().add(this);
    }

    // Devuelve la cantidad al inventario. Llamar con la fila de inventario ya bloqueada.
    public void liberar() {
        if (estado == EstadoCompromiso.LIBERADO) {
            return;
        }
        inventario.liberar(cantidad);
        this.estado = EstadoCompromiso.LIBERADO;
        this.fechaLiberacion = LocalDateTime.now();
    }

    public boolean estaActivo() {
        return estado == EstadoCompromiso.ACTIVO;
    }

    public Long getId() { return id; }
    public Proyecto getProyecto() { return proyecto; }
    public String getOfertaRef() { return ofertaRef; }
    public Ong getOng() { return ong; }
    public RecursoInventario getInventario() { return inventario; }
    public TipoRecurso getTipoRecurso() { return inventario.getTipoRecurso(); }
    public int getCantidad() { return cantidad; }
    public RolAsignado getRolAsignado() { return rolAsignado; }
    public EstadoCompromiso getEstado() { return estado; }
    public LocalDateTime getFechaCompromiso() { return fechaCompromiso; }
    public LocalDateTime getFechaLiberacion() { return fechaLiberacion; }
}
