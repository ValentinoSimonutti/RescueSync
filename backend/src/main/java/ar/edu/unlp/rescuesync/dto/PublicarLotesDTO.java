package ar.edu.unlp.rescuesync.dto;

import java.util.List;

// Lo que manda el coordinador al publicar la convocatoria.
public class PublicarLotesDTO {

    private Integer duracionMinutos;      // duración de la ventana de ofertas
    private List<NuevoLoteDTO> lotes;     // ej: [{recurso:"Paramédicos", cantidad:5}, ...]

    // Constructor vacío: Jackson crea el objeto vacío y después lo llena con los setters
    public PublicarLotesDTO() {
    }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public List<NuevoLoteDTO> getLotes() { return lotes; }
    public void setLotes(List<NuevoLoteDTO> lotes) { this.lotes = lotes; }
}
