package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Certificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {
    Optional<Certificacion> findByCodigo(String codigo);
}
