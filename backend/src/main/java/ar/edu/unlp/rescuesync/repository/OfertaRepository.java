package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    // Ofertas de todos los lotes de una emergencia (para cobertura y para Bonita)
    List<Oferta> findByLote_Emergencia_IdAndEstadoOrderByIdAsc(Long emergenciaId, EstadoOferta estado);
}
