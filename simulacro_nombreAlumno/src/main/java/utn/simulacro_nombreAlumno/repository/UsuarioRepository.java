package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByRol(utn.simulacro_nombreAlumno.model.Rol rol);
    long countByRolAndActivoTrue(utn.simulacro_nombreAlumno.model.Rol rol);
}
