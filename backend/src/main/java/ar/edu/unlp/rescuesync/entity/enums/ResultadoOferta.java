package ar.edu.unlp.rescuesync.entity.enums;

// Lo que decidió el municipio al adjudicar (null mientras no se adjudicó)
public enum ResultadoOferta {
    ADJUDICADA,       // la oferta quedó como responsable del lote
    NO_SELECCIONADA   // el municipio eligió otra oferta (o no estaba validada)
}
