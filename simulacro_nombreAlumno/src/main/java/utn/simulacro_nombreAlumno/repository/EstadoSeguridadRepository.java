package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import utn.simulacro_nombreAlumno.model.EstadoSeguridad;
import java.util.Optional;

public interface EstadoSeguridadRepository extends JpaRepository<EstadoSeguridad,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstadoSeguridad e where e.id = 1")
    Optional<EstadoSeguridad> bloquear();
}
