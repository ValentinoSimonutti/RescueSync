package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface EmergenciaRepository extends JpaRepository<Emergencia,Long> {

    Optional<Emergencia> findByBonitaCaseId(Long caseId);

    List<Emergencia> findByMunicipio_IdOrderByIdDesc(Long municipioId);

    // Convocatorias abiertas para el coordinador
    List<Emergencia> findByEstadoOrderByFechaCierreOfertasAsc(EstadoEmergencia estado);

    List<Emergencia> findByEstadoInOrderByIdDesc(List<EstadoEmergencia> estados);

    // Emergencias creadas antes de que existieran las fechas: se aproximan con la primera oferta
    // (o el cierre de la convocatoria) y con la última finalización de una ONG.
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            update emergencia e set fecha_registro = coalesce(
                (select min(o.fecha_creacion) from oferta o join lote l on o.lote_id = l.id where l.emergencia_id = e.id),
                e.fecha_cierre_ofertas)
            where e.fecha_registro is null
            """)
    int completarFechasRegistro();

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            update emergencia e set fecha_cierre = (
                select max(a.fecha_finalizacion) from aporte a join oferta o on a.oferta_id = o.id
                join lote l on o.lote_id = l.id where l.emergencia_id = e.id)
            where e.estado = 'CERRADA' and e.fecha_cierre is null
            """)
    int completarFechasCierre();
}
