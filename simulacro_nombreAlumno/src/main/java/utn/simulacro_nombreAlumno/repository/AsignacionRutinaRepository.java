package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.AsignacionRutina;

import java.util.List;
import java.util.Optional;

public interface AsignacionRutinaRepository extends JpaRepository<AsignacionRutina, Long> {
    Optional<AsignacionRutina> findByAlumnoAndActivaTrue(Alumno alumno);
    List<AsignacionRutina> findByAlumno(Alumno alumno);
}
