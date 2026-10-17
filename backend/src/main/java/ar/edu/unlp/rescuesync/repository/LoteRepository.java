package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Lote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote,Long> {

    List<Lote> findByEmergencia_IdOrderByIdAsc(Long emergenciaId);

    // Bloquea el lote (SELECT ... FOR UPDATE) para que dos ONGs no ofrezcan a la vez
    // y entre las dos superen lo que pide el lote
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> findByIdParaOfertar(Long id);

}
