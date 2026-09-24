package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergenciaRepository extends JpaRepository<Emergencia,Long> {
}
