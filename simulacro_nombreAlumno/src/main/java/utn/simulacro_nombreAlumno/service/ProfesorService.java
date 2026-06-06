package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProfesorService  {

    private final ProfesorMapper profesorMapper;
    private final ProfesorRepository profesorRepository;

    public ProfesorResponse createProfesor(ProfesorRequest nuevo) {
        Profesor profesor = profesorMapper.toEntity(nuevo);
        profesorRepository.save(profesor);

        ProfesorResponse resp = profesorMapper.toDto(profesor);
        return resp;
    }

    public List<ProfesorResponse> listarProfesores() {
        List<Profesor> profesores = profesorRepository.findAll();
        List<ProfesorResponse> resp = profesorMapper.toLISTDto(profesores);
        return resp;
    }


    public ProfesorResponse updateProfesor(Long id, ProfesorRequest nuevo) {

        Profesor profesor = findEntityById(id);

        profesor.setNombre(nuevo.getNombre());
        profesor.setApellido(nuevo.getApellido());
        profesor.setEmail(nuevo.getEmail());
        profesor.setEspecialidad(nuevo.getEspecialidad());

        profesorRepository.save(profesor);

        return profesorMapper.toDto(profesor);
    }

    public Profesor findEntityById(long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor no encontrado"));
    }

    public ProfesorResponse findProfesorResponseById(long id) {
        return  profesorMapper.toDto(findEntityById(id));
    }

}


