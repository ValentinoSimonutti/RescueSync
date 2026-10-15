package ar.edu.unlp.sistemanacional.entity.enums;

public enum Rol {
    ADMIN,     // personal del Sistema Nacional: habilita ONGs y otorga certificaciones
    SISTEMA,   // cuenta de servicio de RescueSync (la usa Bonita): valida, compromete y libera
    ONG        // representante de una ONG: consulta su perfil y carga su inventario
}
