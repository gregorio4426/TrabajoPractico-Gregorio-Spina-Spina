package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.AlumnoMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

@RequiredArgsConstructor
@Service
public class AlumnoService  {

    private final AlumnoMapper alumnoMapper;
    private final AlumnoRepository alumnoRepository;

    public Alumno findEntityById(long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }

    public AlumnoResponse findRutinaResponseById(long id) {
        return  alumnoMapper.toDto(findEntityById(id));
    }

}


