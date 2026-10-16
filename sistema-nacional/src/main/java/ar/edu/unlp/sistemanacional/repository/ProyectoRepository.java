package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Proyecto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    Optional<Proyecto> findByEmergenciaRef(String emergenciaRef);

    // Serializa las operaciones sobre una misma emergencia (comprometer / liberar / cerrar)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proyecto p where p.emergenciaRef = :emergenciaRef")
    Optional<Proyecto> findForUpdate(String emergenciaRef);

    // Crea el proyecto si no existe. Si dos requests lo crean a la vez, Postgres hace esperar al segundo
    // y no inserta nada (en vez de fallar por el unique): después ambos lo bloquean con findForUpdate.
    @Modifying
    @Query(value = """
            insert into proyecto (emergencia_ref, gravedad, descripcion, estado, fecha_inicio)
            values (:emergenciaRef, :gravedad, :descripcion, 'EN_EJECUCION', :fecha)
            on conflict (emergencia_ref) do nothing
            """, nativeQuery = true)
    void crearSiNoExiste(String emergenciaRef, String gravedad, String descripcion, LocalDateTime fecha);
}
