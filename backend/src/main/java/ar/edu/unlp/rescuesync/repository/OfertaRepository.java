package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Oferta;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    // Ofertas de todos los lotes de una emergencia (para cobertura y para Bonita)
    List<Oferta> findByLote_Emergencia_IdAndEstadoOrderByIdAsc(Long emergenciaId, EstadoOferta estado);

    // Lote de una oferta, sin cargar la oferta (para bloquear el lote antes de leerla)
    @Query("select o.lote.id from Oferta o where o.id = :ofertaId")
    Optional<Long> findLoteIdByOfertaId(Long ofertaId);

    // Lo ofrecido (aportes confirmados) en las ofertas activas del lote, sin contar una oferta
    @Query("""
            select coalesce(sum(o.cantidad), 0) from Oferta o
            where o.lote.id = :loteId
              and o.estado = ar.edu.unlp.rescuesync.entity.enums.EstadoOferta.ACTIVA
              and o.id <> :excluirOfertaId
            """)
    int ofrecidoEnLote(Long loteId, Long excluirOfertaId);
}
