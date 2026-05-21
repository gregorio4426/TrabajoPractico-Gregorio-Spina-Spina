package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

@RequiredArgsConstructor
@Service
public class ProfesorService  {

    private final ProfesorMapper profesorMapper;
    private final ProfesorRepository profesorRepository;

    public Profesor findEntityById(long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }

    public ProfesorResponse findRutinaResponseById(long id) {
        return  profesorMapper.toDto(findEntityById(id));
    }

}


