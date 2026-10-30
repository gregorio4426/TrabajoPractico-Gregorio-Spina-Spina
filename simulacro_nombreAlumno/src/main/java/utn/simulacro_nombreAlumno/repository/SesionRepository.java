package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Sesion;
import java.util.List;

public interface SesionRepository extends JpaRepository<Sesion, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Sesion> {
    @org.springframework.data.jpa.repository.Query("select s from Sesion s where s.alumno = :alumno order by coalesce(s.fechaCorregida,s.fecha) desc, s.id desc")
    List<Sesion> findByAlumnoOrderByFechaDesc(Alumno alumno);
    @org.springframework.data.jpa.repository.Query("select s from Sesion s where s.alumno = :alumno and s.diaRutina.rutina.profesor = :profesor order by coalesce(s.fechaCorregida,s.fecha) desc, s.id desc")
    List<Sesion> findByAlumnoAndDiaRutina_Rutina_ProfesorOrderByFechaDesc(Alumno alumno, Profesor profesor);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Sesion s where s.id = :id")
    java.util.Optional<Sesion> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
