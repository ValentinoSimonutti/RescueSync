package ar.edu.unlp.rescuesync.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Historial de una oferta: una fila por cada cambio (trazabilidad que pide el enunciado).
// Nunca se modifica ni se borra: solo se agregan versiones nuevas.
@Entity
public class OfertaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Oferta oferta;

    private Integer numero;             // versión de la oferta después del cambio
    private LocalDateTime fecha;
    private String autor;               // ONG que hizo el cambio
    private String accion;              // ej: "Modificó su aporte de 2 a 3"
    private Integer total;              // total confirmado después del cambio

    @Column(length = 1000)
    private String detalle;             // foto de los aportes: "ONG 1: 2 · ONG 2: 3 (pendiente)"

    protected OfertaVersion() {
    }

    public OfertaVersion(Oferta oferta, int numero, String autor, String accion, int total, String detalle) {
        this.oferta = oferta;
        this.numero = numero;
        this.fecha = LocalDateTime.now();
        this.autor = autor;
        this.accion = accion;
        this.total = total;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Oferta getOferta() { return oferta; }
    public Integer getNumero() { return numero; }
    public LocalDateTime getFecha() { return fecha; }
    public String getAutor() { return autor; }
    public String getAccion() { return accion; }
    public Integer getTotal() { return total; }
    public String getDetalle() { return detalle; }
}
