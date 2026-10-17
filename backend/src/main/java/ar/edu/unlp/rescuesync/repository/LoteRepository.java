package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote,Long> {

    List<Lote> findByEmergencia_IdOrderByIdAsc(Long emergenciaId);

}
