package ar.edu.unlp.rescuesync.entity;

import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Lo que pone UNA ONG dentro de una oferta.
// Oferta individual = 1 aporte. Consorcio = varios aportes.
@Entity
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Oferta oferta;

    @ManyToOne
    private ONG ong;

    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    private EstadoAporte estado;

    private LocalDateTime fechaActualizacion;

    protected Aporte() {
    }

    public Aporte(Oferta oferta, ONG ong, int cantidad, EstadoAporte estado) {
        this.oferta = oferta;
        this.ong = ong;
        this.cantidad = cantidad;
        this.estado = estado;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void actualizar(int cantidad, EstadoAporte estado) {
        this.cantidad = cantidad;
        this.estado = estado;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Oferta getOferta() { return oferta; }
    public ONG getOng() { return ong; }
    public int getCantidad() { return cantidad == null ? 0 : cantidad; }
    public EstadoAporte getEstado() { return estado; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
