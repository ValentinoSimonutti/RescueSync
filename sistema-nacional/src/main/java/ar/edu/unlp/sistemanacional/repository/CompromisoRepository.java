package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Compromiso;
import ar.edu.unlp.sistemanacional.entity.Proyecto;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoCompromiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    Optional<Compromiso> findByProyectoAndOfertaRefAndOngCuit(Proyecto proyecto, String ofertaRef, String cuit);

    // Ids de inventario de los compromisos activos (cuit null = de todas las ONGs del proyecto).
    // Se piden antes de cargar los compromisos para bloquear esas filas primero.
    @Query("""
            select distinct c.inventario.id from Compromiso c
            where c.proyecto = :proyecto and c.estado = 'ACTIVO' and (:cuit is null or c.ong.cuit = :cuit)
            """)
    List<Long> findInventarioIdsActivos(Proyecto proyecto, String cuit);

    @Query("""
            select c from Compromiso c
            where c.proyecto = :proyecto and c.estado = 'ACTIVO' and (:cuit is null or c.ong.cuit = :cuit)
            order by c.id
            """)
    List<Compromiso> findActivos(Proyecto proyecto, String cuit);

    @Query("""
            select c from Compromiso c
            where (:emergenciaRef is null or c.proyecto.emergenciaRef = :emergenciaRef)
              and (:cuit is null or c.ong.cuit = :cuit)
              and (:estado is null or c.estado = :estado)
            order by c.id
            """)
    List<Compromiso> buscar(String emergenciaRef, String cuit, EstadoCompromiso estado);
}
