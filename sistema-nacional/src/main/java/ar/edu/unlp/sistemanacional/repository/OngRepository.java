package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Ong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OngRepository extends JpaRepository<Ong, Long> {
    Optional<Ong> findByCuit(String cuit);
    boolean existsByCuit(String cuit);
}
