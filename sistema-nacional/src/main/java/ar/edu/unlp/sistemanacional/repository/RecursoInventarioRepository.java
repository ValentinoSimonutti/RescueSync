package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Ong;
import ar.edu.unlp.sistemanacional.entity.RecursoInventario;
import ar.edu.unlp.sistemanacional.entity.TipoRecurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecursoInventarioRepository extends JpaRepository<RecursoInventario, Long> {
    Optional<RecursoInventario> findByOngAndTipoRecurso(Ong ong, TipoRecurso tipoRecurso);
    List<RecursoInventario> findByTipoRecurso(TipoRecurso tipoRecurso);
}
