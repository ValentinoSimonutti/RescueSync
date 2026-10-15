package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ONG registrada a nivel nacional. Se identifica por CUIT: es la clave que comparte con
// la app local de RescueSync (la ONG local guarda el mismo CUIT).
@Entity
public class Ong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Solo dígitos (11), sin guiones
    @Column(unique = true, nullable = false, length = 11)
    private String cuit;

    private String nombre;
    private String email;
    private String provincia;

    @Enumerated(EnumType.STRING)
    private EstadoOng estado;

    private LocalDateTime fechaAlta;
    private LocalDateTime fechaCambioEstado;
    private String motivoEstado;

    @OneToMany(mappedBy = "ong")
    private List<OngCertificacion> certificaciones = new ArrayList<>();

    @OneToMany(mappedBy = "ong")
    private List<RecursoInventario> inventario = new ArrayList<>();

    protected Ong() {
    }

    public Ong(String cuit, String nombre, String email, String provincia) {
        this.cuit = cuit;
        this.nombre = nombre;
        this.email = email;
        this.provincia = provincia;
        this.estado = EstadoOng.PENDIENTE;
        this.fechaAlta = LocalDateTime.now();
        this.fechaCambioEstado = this.fechaAlta;
    }

    public void cambiarEstado(EstadoOng nuevo, String motivo) {
        this.estado = nuevo;
        this.motivoEstado = motivo;
        this.fechaCambioEstado = LocalDateTime.now();
    }

    public boolean estaHabilitada() {
        return estado == EstadoOng.HABILITADA;
    }

    public Long getId() { return id; }
    public String getCuit() { return cuit; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getProvincia() { return provincia; }
    public EstadoOng getEstado() { return estado; }
    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public LocalDateTime getFechaCambioEstado() { return fechaCambioEstado; }
    public String getMotivoEstado() { return motivoEstado; }
    public List<OngCertificacion> getCertificaciones() { return certificaciones; }
    public List<RecursoInventario> getInventario() { return inventario; }
}
