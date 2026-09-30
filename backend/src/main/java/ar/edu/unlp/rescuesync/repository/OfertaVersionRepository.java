package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.OfertaVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaVersionRepository extends JpaRepository<OfertaVersion, Long> {

    // Historial de una oferta, de la versión más nueva a la más vieja
    List<OfertaVersion> findByOferta_IdOrderByNumeroDesc(Long ofertaId);
}
