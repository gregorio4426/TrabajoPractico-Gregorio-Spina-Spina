package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Rutina;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {
}

