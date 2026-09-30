package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.ONG;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ONGRepository extends JpaRepository<ONG, Long> {

    // Para invitar a un consorcio: todas las ONGs menos la propia
    List<ONG> findByIdNotOrderByNombreAsc(Long id);
}
