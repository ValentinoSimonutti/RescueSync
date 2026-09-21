package ar.edu.unlp.rescuesync.entity;

import ar.edu.unlp.rescuesync.entity.enums.Gravedad;
import ar.edu.unlp.rescuesync.entity.enums.TipoDesastre;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public class Emergencia {

    private Long id;

    private String zonaAfectada;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoDesastre tipoDesastre;

    @Enumerated(EnumType.STRING)
    private Gravedad gravedad;

    private Municipio municipio;

    public Emergencia(){
    }






}
