package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.EstadoProyecto;
import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Una emergencia de RescueSync vista desde el Sistema Nacional: agrupa los compromisos de recursos.
// emergenciaRef es el id de la emergencia en la app local (lo manda Bonita).
// Se crea con el primer compromiso y se cierra cuando se liberan todos los recursos.
@Entity
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String emergenciaRef;

    @Enumerated(EnumType.STRING)
    private NivelRiesgo gravedad;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private EstadoProyecto estado;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;

    @OneToMany(mappedBy = "proyecto")
    @OrderBy("id")
    private List<Compromiso> compromisos = new ArrayList<>();

    protected Proyecto() {
    }

    public Proyecto(String emergenciaRef, NivelRiesgo gravedad, String descripcion) {
        this.emergenciaRef = emergenciaRef;
        this.gravedad = gravedad;
        this.descripcion = descripcion;
        this.estado = EstadoProyecto.EN_EJECUCION;
        this.fechaInicio = LocalDateTime.now();
    }

    public boolean estaCerrado() {
        return estado == EstadoProyecto.CERRADO;
    }

    public void cerrar() {
        this.estado = EstadoProyecto.CERRADO;
        this.fechaCierre = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEmergenciaRef() { return emergenciaRef; }
    public NivelRiesgo getGravedad() { return gravedad; }
    public String getDescripcion() { return descripcion; }
    public EstadoProyecto getEstado() { return estado; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public List<Compromiso> getCompromisos() { return compromisos; }
}
