package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.AsignacionRutina;

import java.util.List;
import java.util.Optional;

public interface AsignacionRutinaRepository extends JpaRepository<AsignacionRutina, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<AsignacionRutina> {
    Optional<AsignacionRutina> findByAlumnoAndActivaTrue(Alumno alumno);
    @org.springframework.data.jpa.repository.Query("select a from AsignacionRutina a where a.alumno = :alumno order by case when a.fechaInicio is null then 1 else 0 end, a.fechaInicio desc, a.id desc")
    List<AsignacionRutina> findByAlumno(Alumno alumno);
    @org.springframework.data.jpa.repository.Query("select a from AsignacionRutina a where a.activa = true and (a.rutina.id = :id or a.rutina.raiz.id = :id)")
    List<AsignacionRutina> findActivasDeFamilia(@org.springframework.data.repository.query.Param("id") Long id);
}
