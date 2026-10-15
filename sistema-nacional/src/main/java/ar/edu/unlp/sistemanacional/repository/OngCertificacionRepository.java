package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Certificacion;
import ar.edu.unlp.sistemanacional.entity.Ong;
import ar.edu.unlp.sistemanacional.entity.OngCertificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OngCertificacionRepository extends JpaRepository<OngCertificacion, Long> {
    Optional<OngCertificacion> findByOngAndCertificacion(Ong ong, Certificacion certificacion);
}
