package ar.edu.unlp.rescuesync.dto;

public class LoteDTO {

    private Long id;
    private String recurso;
    private String recursoCodigo;
    private int cantidad;
    private Long emergenciaId;

    public LoteDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public String getRecursoCodigo() {
        return recursoCodigo;
    }

    public void setRecursoCodigo(String recursoCodigo) {
        this.recursoCodigo = recursoCodigo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Long getEmergenciaId() {
        return emergenciaId;
    }

    public void setEmergenciaId(Long emergenciaId) {
        this.emergenciaId = emergenciaId;
    }
}
