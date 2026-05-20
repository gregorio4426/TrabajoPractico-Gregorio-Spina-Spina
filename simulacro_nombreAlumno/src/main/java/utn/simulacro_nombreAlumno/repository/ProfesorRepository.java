package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Profesor;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
}

