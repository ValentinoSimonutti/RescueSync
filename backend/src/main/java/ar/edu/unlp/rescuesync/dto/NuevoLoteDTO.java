package ar.edu.unlp.rescuesync.dto;

public class NuevoLoteDTO {

    private String recurso;
    private int cantidad;

    public NuevoLoteDTO() {
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}