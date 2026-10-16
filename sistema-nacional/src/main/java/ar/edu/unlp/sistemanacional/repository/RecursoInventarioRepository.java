package ar.edu.unlp.sistemanacional.repository;

import ar.edu.unlp.sistemanacional.entity.Ong;
import ar.edu.unlp.sistemanacional.entity.RecursoInventario;
import ar.edu.unlp.sistemanacional.entity.TipoRecurso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecursoInventarioRepository extends JpaRepository<RecursoInventario, Long> {
    Optional<RecursoInventario> findByOngAndTipoRecurso(Ong ong, TipoRecurso tipoRecurso);
    List<RecursoInventario> findByTipoRecurso(TipoRecurso tipoRecurso);

    // Solo el id (no carga la entidad): se usa para saber qué filas bloquear ANTES de leerlas
    @Query("select r.id from RecursoInventario r where r.ong.cuit = :cuit and r.tipoRecurso.codigo = :codigo")
    Optional<Long> findIdByCuitAndTipo(String cuit, String codigo);

    // SELECT ... FOR UPDATE: otra transacción que quiera las mismas filas espera a que esta termine.
    // Siempre en orden de id, así dos transacciones nunca se bloquean en orden cruzado (deadlock).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecursoInventario r where r.id in :ids order by r.id")
    List<RecursoInventario> findAllByIdForUpdate(Collection<Long> ids);
}
