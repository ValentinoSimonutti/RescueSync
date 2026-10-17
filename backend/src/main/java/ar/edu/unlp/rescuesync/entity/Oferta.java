package ar.edu.unlp.rescuesync.entity;

import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.ResultadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.RolValidacion;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Una propuesta de ayuda para UN lote. Puede ser:
//  - individual: 1 aporte (una sola ONG)
//  - consorcio: varios aportes (varias ONGs asociadas que responden juntas)
// Lo que se evalúa y adjudica es la oferta entera.
@Entity
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Total ofrecido = suma de los aportes CONFIRMADOS (se recalcula en cada cambio)
    private int cantidad;

    // Recurso del lote (ej: "Paramédicos")
    private String recurso;

    // ONG líder: la que creó la oferta (en un consorcio, la que invitó a las demás)
    @ManyToOne
    private ONG ong;

    @ManyToOne
    private Lote lote;

    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;

    // Número de versión: sube en cada cambio (trazabilidad)
    private Integer numeroVersion;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Validación del Sistema Nacional (null hasta que Bonita manda el resultado)
    @Enumerated(EnumType.STRING)
    private RolValidacion rolValidacion;
    private Integer cantidadValidada;
    @Column(length = 2000)
    private String observacionesValidacion;

    // Adjudicación del municipio (null hasta que adjudica)
    @Enumerated(EnumType.STRING)
    private ResultadoOferta resultado;

    // Qué pone cada ONG. cascade: al guardar la oferta se guardan sus aportes
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aporte> aportes = new ArrayList<>();

    protected Oferta() {
    }

    public Oferta(ONG lider, Lote lote) {
        this.ong = lider;
        this.lote = lote;
        this.recurso = lote.getRecurso();
        this.estado = EstadoOferta.ACTIVA;
        this.numeroVersion = 0;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public void agregarAporte(Aporte aporte) {
        aportes.add(aporte);
    }

    // Suma solo lo confirmado: una invitación pendiente todavía no cuenta
    public void recalcularTotal() {
        this.cantidad = aportes.stream()
                .filter(a -> a.getEstado() == EstadoAporte.CONFIRMADO)
                .mapToInt(Aporte::getCantidad)
                .sum();
    }

    // Cada cambio genera una versión nueva
    public int nuevaVersion() {
        this.numeroVersion = (numeroVersion == null ? 0 : numeroVersion) + 1;
        this.fechaActualizacion = LocalDateTime.now();
        return numeroVersion;
    }

    public Optional<Aporte> aporteDe(Long ongId) {
        return aportes.stream().filter(a -> a.getOng().getId().equals(ongId)).findFirst();
    }

    public boolean esLider(Long ongId) {
        return ong != null && ong.getId().equals(ongId);
    }

    // Consorcio = más de una ONG que confirmó o fue invitada (sin contar rechazos/retiros)
    public boolean esConsorcio() {
        return aportes.stream()
                .filter(a -> a.getEstado() == EstadoAporte.CONFIRMADO || a.getEstado() == EstadoAporte.PENDIENTE)
                .count() > 1;
    }

    public void registrarValidacion(RolValidacion rol, int cantidadValidada, String observaciones) {
        this.rolValidacion = rol;
        this.cantidadValidada = cantidadValidada;
        this.observacionesValidacion = observaciones;
    }

    // El municipio solo ve (y elige) las ofertas validadas que cubren el lote completo
    public boolean esAdjudicable() {
        return estado == EstadoOferta.ACTIVA
                && rolValidacion != null && rolValidacion != RolValidacion.SIN_HABILITACION_VIGENTE
                && cantidadValidada != null && cantidadValidada >= lote.getCantidad();
    }

    public Long getId() { return id; }
    public int getCantidad() { return cantidad; }
    public String getRecurso() { return recurso; }
    public ONG getOng() { return ong; }
    public Lote getLote() { return lote; }
    public EstadoOferta getEstado() { return estado; }
    public void setEstado(EstadoOferta estado) { this.estado = estado; }
    public Integer getNumeroVersion() { return numeroVersion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public List<Aporte> getAportes() { return aportes; }
    public RolValidacion getRolValidacion() { return rolValidacion; }
    public Integer getCantidadValidada() { return cantidadValidada; }
    public String getObservacionesValidacion() { return observacionesValidacion; }
    public ResultadoOferta getResultado() { return resultado; }
    public void setResultado(ResultadoOferta resultado) { this.resultado = resultado; }
}
