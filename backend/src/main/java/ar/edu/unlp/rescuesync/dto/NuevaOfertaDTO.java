package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Lo que manda la ONG al crear una oferta.
// La ONG que crea NO viaja acá: sale de la sesión (y queda como líder).
// El recurso tampoco: se toma del lote.
public class NuevaOfertaDTO {

    private Long loteId;                  // a qué lote se ofrece
    private int cantidad;                 // cuánto aporta la ONG que crea la oferta
    private List<Long> ongsInvitadas;     // opcional: ONGs a invitar para armar un consorcio

    public NuevaOfertaDTO() {
    }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public List<Long> getOngsInvitadas() { return ongsInvitadas; }
    public void setOngsInvitadas(List<Long> ongsInvitadas) { this.ongsInvitadas = ongsInvitadas; }
}
