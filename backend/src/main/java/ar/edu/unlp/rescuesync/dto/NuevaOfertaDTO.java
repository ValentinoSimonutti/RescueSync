package ar.edu.unlp.rescuesync.dto;

// Lo que manda la ONG al ofrecer ayuda: a qué lote y cuánto puede aportar.
// La ONG NO viaja acá: sale de la sesión en el backend.
// El recurso tampoco: se toma del lote (la ONG ofrece lo que el lote pide).
public class NuevaOfertaDTO {

    private Long loteId;      // a qué lote se ofrece
    private int cantidad;     // cuánto aporta (puede ser parcial: menos de lo que pide el lote)

    public NuevaOfertaDTO() {
    }

    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
