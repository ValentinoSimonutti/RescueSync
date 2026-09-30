package ar.edu.unlp.rescuesync.repository;

import ar.edu.unlp.rescuesync.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Une los dos mundos: el usuario que se logueó en Bonita -> su perfil en Postgres
    Optional<Usuario> findByBonitaUsername(String bonitaUsername);
}