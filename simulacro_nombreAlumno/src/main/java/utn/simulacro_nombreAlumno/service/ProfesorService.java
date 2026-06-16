package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterProfesorRequest;
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

    public Profesor createProfesor(RegisterProfesorRequest request, Usuario usuario) {
        Profesor profesor = Profesor.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .especialidad(request.getEspecialidad())
                .usuario(usuario)
                .build();
        return profesorRepository.save(profesor);
    }

    public List<ProfesorResponse> listarProfesores() {
        return profesorMapper.toLISTDto(profesorRepository.findAll());
    }

    // SCRUM-26 — Modificar perfil profesor
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
        return profesorMapper.toDto(findEntityById(id));
    }


    public java.util.Optional<Profesor> findByUsuario(utn.simulacro_nombreAlumno.model.Usuario usuario) {
        return profesorRepository.findByUsuario(usuario);
    }
}


