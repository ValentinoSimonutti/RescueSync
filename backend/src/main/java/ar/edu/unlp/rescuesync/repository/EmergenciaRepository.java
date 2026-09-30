package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.entity.Emergencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergenciaRepository extends JpaRepository<Emergencia,Long> {

    Optional<Emergencia> findByBonitaCaseId(Long caseId);
}
