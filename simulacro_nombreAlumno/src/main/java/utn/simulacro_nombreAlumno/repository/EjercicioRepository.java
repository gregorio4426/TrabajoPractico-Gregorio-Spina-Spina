package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Ejercicio;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
}

