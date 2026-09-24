 package ar.edu.unlp.rescuesync.entity;

    import jakarta.persistence.Entity;

    @Entity
    public class CentroCoordinador extends Organizacion {
        // es el que va a publicar los lotes

        protected CentroCoordinador() {
        }

        public CentroCoordinador(String nombre) {
            super(nombre);
        }
    }