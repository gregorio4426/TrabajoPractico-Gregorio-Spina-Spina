package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Usuario;

import java.util.Optional;

public interface ProfesorRepository extends JpaRepository<Profesor, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Profesor> {
    Optional<Profesor> findByUsuario(Usuario usuario);

    @org.springframework.data.jpa.repository.Query("select count(p) from Profesor p where lower(trim(p.email)) = :email and (p.usuario is null or p.usuario.id <> :usuarioId)")
    long countEmailOcupado(@org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
