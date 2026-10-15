package ar.edu.unlp.sistemanacional.entity;

import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import jakarta.persistence.*;

import java.time.LocalDate;

// Certificación otorgada a una ONG, con nivel y vencimiento.
// Una certificación vencida no se borra: sigue a la vista pero deja de habilitar.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"ong_id", "certificacion_id"}))
public class OngCertificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ong ong;

    @ManyToOne(optional = false)
    private Certificacion certificacion;

    @Enumerated(EnumType.STRING)
    private NivelCompetencia nivel;

    private LocalDate fechaOtorgamiento;
    private LocalDate vigenteHasta;

    protected OngCertificacion() {
    }

    public OngCertificacion(Ong ong, Certificacion certificacion, NivelCompetencia nivel, LocalDate vigenteHasta) {
        this.ong = ong;
        this.certificacion = certificacion;
        this.nivel = nivel;
        this.fechaOtorgamiento = LocalDate.now();
        this.vigenteHasta = vigenteHasta;
        ong.getCertificaciones().add(this);
    }

    // Renovar / cambiar de nivel: se pisa la misma fila (una certificación por ONG)
    public void renovar(NivelCompetencia nivel, LocalDate vigenteHasta) {
        this.nivel = nivel;
        this.vigenteHasta = vigenteHasta;
        this.fechaOtorgamiento = LocalDate.now();
    }

    public boolean estaVigente(LocalDate hoy) {
        return vigenteHasta == null || !vigenteHasta.isBefore(hoy);
    }

    public Long getId() { return id; }
    public Ong getOng() { return ong; }
    public Certificacion getCertificacion() { return certificacion; }
    public NivelCompetencia getNivel() { return nivel; }
    public LocalDate getFechaOtorgamiento() { return fechaOtorgamiento; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
}
