package utn.simulacro_nombreAlumno.service;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.EjercicioMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Ejercicio;
import utn.simulacro_nombreAlumno.model.GrupoMuscular;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.request.EjercicioRequest;
import utn.simulacro_nombreAlumno.model.response.EjercicioResponse;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.repository.EjercicioRepository;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class EjercicioService {

    private final EjercicioMapper ejercicioMapper;
    private final EjercicioRepository ejercicioRepository;
    private final utn.simulacro_nombreAlumno.repository.DiaEjercicioRepository prescripciones;


    @PreAuthorize("hasRole('PROFESOR')")
    public EjercicioResponse createEjercicio(EjercicioRequest nuevo) {
        Ejercicio ejercicio = ejercicioMapper.toEntity(nuevo);
        ejercicioRepository.save(ejercicio);

        EjercicioResponse ejercicioResponse = ejercicioMapper.toDto(ejercicio);
        return ejercicioResponse;
    }

    public List<EjercicioResponse> listarEjercicios() {
        List<Ejercicio> ejercicios = ejercicioRepository.findAll();
        List<EjercicioResponse> ejercicioResponse = ejercicioMapper.toLISTDto(ejercicios);
        return ejercicioResponse;
    }

    @PreAuthorize("hasRole('PROFESOR')")
    public EjercicioResponse updateEjercicio(Long id, EjercicioRequest request) {
        Ejercicio ejercicio = findEntityById(id);
        ejercicio.setNombre(request.getNombre());
        ejercicio.setDescripcion(request.getDescripcion());
        ejercicio.setGrupoMuscular(request.getGrupoMuscular());
        ejercicioRepository.save(ejercicio);
        return ejercicioMapper.toDto(ejercicio);
    }

    @PreAuthorize("hasRole('PROFESOR')")
    public void deleteEjercicio(Long id) {
        if (prescripciones.existsByEjercicio_Id(id)) {
            throw new utn.simulacro_nombreAlumno.exception.ReglaNegocioException("El ejercicio está referenciado por rutinas o historial y no puede eliminarse");
        }
        ejercicioRepository.delete(findEntityById(id));
    }

    public Ejercicio findEntityById(long id) {
        return ejercicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }
    public List<EjercicioResponse> listarPorGrupoMuscular(GrupoMuscular grupoMuscular) {
        List<Ejercicio> ejercicios = ejercicioRepository.findByGrupoMuscular(grupoMuscular);
        return ejercicioMapper.toLISTDto(ejercicios);
    }

    public EjercicioResponse findEjercicioResponseById(long id) {
        return  ejercicioMapper.toDto(findEntityById(id));
    }

    public List<EjercicioResponse> listarPorNombre(String nombre) {
        return ejercicioMapper.toLISTDto(
                ejercicioRepository.findByNombreContainingIgnoreCase(nombre)
        );
    }

}


