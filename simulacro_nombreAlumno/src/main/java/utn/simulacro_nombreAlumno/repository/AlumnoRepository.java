package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Usuario;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlumnoRepository extends JpaRepository<Alumno, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Alumno> {
    boolean existsByIdAndProfesores_Id(Long id, Long profesorId);
    List<Alumno> findDistinctByProfesores_IdOrderByIdAsc(Long profesorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alumno a where a.id = :id")
    Optional<Alumno> findByIdForUpdate(@Param("id") Long id);

    Optional<Alumno> findByUsuario(Usuario usuario);

    @org.springframework.data.jpa.repository.Query("select count(p) from Alumno p where lower(trim(p.email)) = :email and (p.usuario is null or p.usuario.id <> :usuarioId)")
    long countEmailOcupado(@org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
