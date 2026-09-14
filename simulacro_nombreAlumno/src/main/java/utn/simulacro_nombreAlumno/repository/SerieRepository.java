package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Serie;

public interface SerieRepository extends JpaRepository<Serie, Long> {
}