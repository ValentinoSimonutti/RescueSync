package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Aporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AporteRepository extends JpaRepository<Aporte, Long> {

    // ¿Esta ONG ya participa (confirmada o invitada) en alguna oferta activa de este lote?
    boolean existsByOng_IdAndOferta_Lote_IdAndOferta_EstadoAndEstadoIn(
            Long ongId, Long loteId, EstadoOferta estadoOferta, Collection<EstadoAporte> estados);

    // Participaciones vivas de una ONG (para su pantalla: sus ofertas e invitaciones)
    List<Aporte> findByOng_IdAndOferta_EstadoAndEstadoIn(
            Long ongId, EstadoOferta estadoOferta, Collection<EstadoAporte> estados);
}
