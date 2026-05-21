package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.EjercicioMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Ejercicio;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.response.EjercicioResponse;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.repository.EjercicioRepository;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

@RequiredArgsConstructor
@Service
public class EjercicioService {

    private final EjercicioMapper ejercicioMapper;
    private final EjercicioRepository ejercicioRepository;

    public Ejercicio findEntityById(long id) {
        return ejercicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }

    public EjercicioResponse findRutinaResponseById(long id) {
        return  ejercicioMapper.toDto(findEntityById(id));
    }
}


