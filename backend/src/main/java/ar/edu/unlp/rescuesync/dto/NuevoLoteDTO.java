package ar.edu.unlp.rescuesync.dto;

public class NuevoLoteDTO {

    private String recursoCodigo;   // código del catálogo nacional; el nombre lo completa el backend
    private int cantidad;

    public NuevoLoteDTO() {
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
}
