package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.TipoRecurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoRecursoRepository extends JpaRepository<TipoRecurso, Long> {
    Optional<TipoRecurso> findByCodigo(String codigo);
}
