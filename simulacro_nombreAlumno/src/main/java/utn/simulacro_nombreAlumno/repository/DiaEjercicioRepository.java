package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.DiaEjercicio;

public interface DiaEjercicioRepository extends JpaRepository<DiaEjercicio, Long> {
}