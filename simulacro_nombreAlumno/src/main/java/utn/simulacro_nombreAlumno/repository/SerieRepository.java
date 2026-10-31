package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Serie;
import java.util.List;

public interface SerieRepository extends JpaRepository<Serie, Long> {
    @org.springframework.data.jpa.repository.Query("select s from Serie s where s.sesion.id = :sesionId and s.diaEjercicio.ejercicio.id = :ejercicioId and s.anulada = false and s.legadoExento = false and s.completada = true order by s.numeroSerie asc, s.id asc")
    List<Serie> findBySesion_IdAndDiaEjercicio_Ejercicio_IdOrderByNumeroSerieAsc(Long sesionId, Long ejercicioId);
    @org.springframework.data.jpa.repository.Query("select s from Serie s where s.sesion.alumno = :alumno and s.diaEjercicio.ejercicio.id = :ejercicioId and s.sesion.completada = true and s.anulada = false and s.legadoExento = false and s.completada = true order by coalesce(s.sesion.fechaCorregida,s.sesion.fecha) desc, s.sesion.id desc, s.numeroSerie asc, s.id asc")
    List<Serie> findBySesion_AlumnoAndDiaEjercicio_Ejercicio_IdAndSesion_CompletadaTrueOrderBySesion_FechaDescIdDesc(Alumno alumno, Long ejercicioId);
    boolean existsBySesion_IdAndDiaEjercicio_Ejercicio_IdAndNumeroSerieAndAnuladaFalseAndIdNot(Long sesionId, Long ejercicioId, Integer numeroSerie, Long id);
}
