package utn.simulacro_nombreAlumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utn.simulacro_nombreAlumno.model.Rutina;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface RutinaRepository extends JpaRepository<Rutina, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Rutina> {
    List<Rutina> findByRaizIsNullAndArchivadaFalseOrderByIdAsc();
    Optional<Rutina> findFirstByRaiz_IdOrderByNumeroVersionDesc(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rutina> findTopByRaiz_IdOrderByNumeroVersionDesc(Long id);
    List<Rutina> findByRaiz_IdOrderByNumeroVersionAsc(Long id);
    Optional<Rutina> findByRaiz_IdAndNumeroVersion(Long id, int numeroVersion);

}
