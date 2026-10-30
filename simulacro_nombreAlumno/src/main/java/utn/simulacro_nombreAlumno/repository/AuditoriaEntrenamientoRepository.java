package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.AuditoriaEntrenamiento;
import java.util.List;

public interface AuditoriaEntrenamientoRepository extends JpaRepository<AuditoriaEntrenamiento, Long> {
    List<AuditoriaEntrenamiento> findBySesion_IdOrderByIdAsc(Long sesionId);
}
