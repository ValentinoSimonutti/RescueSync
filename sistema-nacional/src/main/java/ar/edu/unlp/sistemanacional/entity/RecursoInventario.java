package ar.edu.unlp.sistemanacional.entity;

import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

// Cuánto tiene una ONG de un tipo de recurso y cuánto ya está comprometido en emergencias.
// Es la fila que se bloquea (SELECT ... FOR UPDATE) al comprometer, para evitar sobre-asignaciones.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"ong_id", "tipo_recurso_id"}))
public class RecursoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ong ong;

    @ManyToOne(optional = false)
    private TipoRecurso tipoRecurso;

    private int cantidadTotal;
    private int cantidadComprometida;

    // Además del lock pesimista: si dos transacciones pisaran la misma fila sin lock, falla la segunda
    @Version
    private Long version;

    private LocalDateTime fechaActualizacion;

    protected RecursoInventario() {
    }

    public RecursoInventario(Ong ong, TipoRecurso tipoRecurso, int cantidadTotal) {
        this.ong = ong;
        this.tipoRecurso = tipoRecurso;
        this.cantidadTotal = cantidadTotal;
        this.cantidadComprometida = 0;
        this.fechaActualizacion = LocalDateTime.now();
        ong.getInventario().add(this);
    }

    public int getDisponible() {
        return cantidadTotal - cantidadComprometida;
    }

    // La ONG declara cuánto tiene. No puede bajar de lo que ya está comprometido.
    public void actualizarTotal(int nuevoTotal) {
        if (nuevoTotal < cantidadComprometida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede bajar el total de " + tipoRecurso.getCodigo() + " a " + nuevoTotal
                            + ": hay " + cantidadComprometida + " comprometidos en emergencias");
        }
        this.cantidadTotal = nuevoTotal;
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Llamar solo con la fila bloqueada (findAllByIdForUpdate). El service ya chequeó el disponible;
    // esto es la última barrera para que nunca quede comprometido > total.
    public void comprometer(int cantidad) {
        if (cantidad > getDisponible()) {
            throw new IllegalStateException("Sobre-asignación de " + tipoRecurso.getCodigo() + " en ONG " + ong.getCuit());
        }
        this.cantidadComprometida += cantidad;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void liberar(int cantidad) {
        this.cantidadComprometida = Math.max(0, cantidadComprometida - cantidad);
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Ong getOng() { return ong; }
    public TipoRecurso getTipoRecurso() { return tipoRecurso; }
    public int getCantidadTotal() { return cantidadTotal; }
    public int getCantidadComprometida() { return cantidadComprometida; }
    public Long getVersion() { return version; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
