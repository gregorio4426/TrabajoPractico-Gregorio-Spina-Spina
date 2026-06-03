package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.EjercicioMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Ejercicio;
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

    public Ejercicio findEntityById(long id) {
        return ejercicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }

    public EjercicioResponse findEjercicioResponseById(long id) {
        return  ejercicioMapper.toDto(findEntityById(id));
    }
}


