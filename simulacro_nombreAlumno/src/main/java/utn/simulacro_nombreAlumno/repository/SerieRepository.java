package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Serie;
import java.util.List;

public interface SerieRepository extends JpaRepository<Serie, Long> {
    List<Serie> findBySesion_IdAndDiaEjercicio_Ejercicio_IdOrderByNumeroSerieAsc(Long sesionId, Long ejercicioId);
    List<Serie> findBySesion_AlumnoAndDiaEjercicio_Ejercicio_IdAndSesion_CompletadaTrueOrderBySesion_FechaDescIdDesc(Alumno alumno, Long ejercicioId);
}
