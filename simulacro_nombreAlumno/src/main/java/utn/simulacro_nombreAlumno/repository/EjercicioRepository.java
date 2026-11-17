package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Ejercicio;
import utn.simulacro_nombreAlumno.model.GrupoMuscular;

import java.util.List;


public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
    boolean existsByGrupoPersonalizado_Id(Long id);
    List<Ejercicio> findByGrupoMuscular(GrupoMuscular grupoMuscular);

    List<Ejercicio> findByNombreContainingIgnoreCase(String nombre);
}

