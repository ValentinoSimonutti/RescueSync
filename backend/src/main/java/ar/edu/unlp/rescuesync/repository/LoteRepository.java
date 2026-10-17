package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote,Long> {

    // Los lotes de la convocatoria actual (si se reabrió, los anteriores quedan archivados)
    @Query("select l from Lote l where l.emergencia.id = :emergenciaId and (l.vigente is null or l.vigente = true) order by l.id")
    List<Lote> vigentesDe(@Param("emergenciaId") Long emergenciaId);

}
