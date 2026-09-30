package ar.edu.unlp.rescuesync.dto;

// Una tarea pendiente de la bandeja: lo que dice Bonita (qué tarea)
// + lo que dice Postgres (de qué emergencia).
public class TareaDTO {

    private Long tareaId;              // id de la tarea en Bonita (para ejecutarla después)
    private String nombreTarea;        // ej: "Generar lotes"
    private EmergenciaDTO emergencia;  // datos de la emergencia (DTO, NO la entidad)

    public TareaDTO(Long tareaId, String nombreTarea, EmergenciaDTO emergencia) {
        this.tareaId = tareaId;
        this.nombreTarea = nombreTarea;
        this.emergencia = emergencia;
    }

    public Long getTareaId() { return tareaId; }
    public String getNombreTarea() { return nombreTarea; }
    public EmergenciaDTO getEmergencia() { return emergencia; }
}
