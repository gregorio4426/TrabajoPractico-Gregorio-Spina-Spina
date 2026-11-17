package utn.simulacro_nombreAlumno.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.GrupoMuscularCatalogo;
import java.util.Optional;
public interface GrupoMuscularRepository extends JpaRepository<GrupoMuscularCatalogo,Long> {
 Optional<GrupoMuscularCatalogo> findByNormalizado(String nombre);
}
