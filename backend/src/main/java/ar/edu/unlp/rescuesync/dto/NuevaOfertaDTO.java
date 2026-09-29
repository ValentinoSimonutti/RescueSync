package ar.edu.unlp.rescuesync.dto;

public class NuevaOfertaDTO {

    private int cantidad;
    private String recurso;

    public NuevaOfertaDTO() {
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }
}