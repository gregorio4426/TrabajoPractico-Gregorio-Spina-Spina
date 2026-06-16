package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.AlumnoMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.AlumnoRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterAlumnoRequest;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AlumnoService  {

    private final AlumnoMapper alumnoMapper;
    private final AlumnoRepository alumnoRepository;
    private final ProfesorService profesorService;


    public Alumno createAlumno(RegisterAlumnoRequest request, Usuario usuario) {
        Alumno alumno = Alumno.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .edad(request.getEdad())
                .fechaNacimiento(request.getFechaNacimiento())
                .nivel(request.getNivel())
                .objetivo(request.getObjetivo())
                .usuario(usuario)
                .build();
        return alumnoRepository.save(alumno);
    }

    public AlumnoResponse updateAlumno(Long id, AlumnoRequest request) {
        Alumno alumno = findEntityById(id);
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEmail(request.getEmail());
        alumno.setEdad(request.getEdad());
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setNivel(request.getNivel());
        alumno.setObjetivo(request.getObjetivo());
        alumnoRepository.save(alumno);
        return alumnoMapper.toDto(alumno);
    }

    public List<AlumnoResponse> listarAlumnos() {
        return alumnoMapper.toLISTDto(alumnoRepository.findAll());
    }

        public AlumnoResponse patchNivel(Long id, Nivel nuevo) {

            Alumno alumno = findEntityById(id);
            alumno.setNivel(nuevo);
            alumnoRepository.save(alumno);

            return alumnoMapper.toDto(alumno);
        }

        public AlumnoResponse patchObjetivo(Long id, Objetivo nuevo) {

            Alumno alumno = findEntityById(id);
            alumno.setObjetivo(nuevo);
            alumnoRepository.save(alumno);

            return alumnoMapper.toDto(alumno);
        }
    public AlumnoResponse elegirProfesor(Long alumnoId, Long profesorId) {
        Alumno alumno = findEntityById(alumnoId);
        Profesor profesor = profesorService.findEntityById(profesorId);
        if (!alumno.getProfesores().contains(profesor)) {
            alumno.getProfesores().add(profesor);
        }
        alumnoRepository.save(alumno);
        return alumnoMapper.toDto(alumno);
    }


    public Alumno findEntityById(long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }

    public AlumnoResponse findAlumnoResponseById(long id) {
        return  alumnoMapper.toDto(findEntityById(id));
    }
    public java.util.Optional<Alumno> findByUsuario(utn.simulacro_nombreAlumno.model.Usuario usuario) {
        return alumnoRepository.findByUsuario(usuario);
    }
}


