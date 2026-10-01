package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Sesion;
import java.util.List;

public interface SesionRepository extends JpaRepository<Sesion, Long> {
    List<Sesion> findByAlumnoOrderByFechaDesc(Alumno alumno);
    List<Sesion> findByAlumnoAndDiaRutina_Rutina_ProfesorOrderByFechaDesc(Alumno alumno, Profesor profesor);
}
