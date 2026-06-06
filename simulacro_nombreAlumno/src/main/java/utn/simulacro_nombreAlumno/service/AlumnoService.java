package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.AlumnoMapper;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Nivel;
import utn.simulacro_nombreAlumno.model.Objetivo;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.request.AlumnoRequest;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AlumnoService  {

    private  AlumnoMapper alumnoMapper;
    private  AlumnoRepository alumnoRepository;


        public AlumnoResponse createAlumno(AlumnoRequest nuevo) {
            Alumno alumno = alumnoMapper.toEntity(nuevo);
            alumnoRepository.save(alumno);
            AlumnoResponse alumnoResponse = alumnoMapper.toDto(alumno);
            return alumnoResponse;

        }

    public AlumnoResponse updateAlumno(Long id, AlumnoRequest request) {
        Alumno alumno = findEntityById(id);
        alumno = alumnoMapper.toEntity(request);
        alumnoRepository.save(alumno);
        return alumnoMapper.toDto(alumno);
    }


        public List<AlumnoResponse> listarAlumnos() {
            List<Alumno> alumnos = alumnoRepository.findAll();
            List<AlumnoResponse> alumnoResponse = alumnoMapper.toLISTDto(alumnos);
            return alumnoResponse;
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


    public Alumno findEntityById(long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }

    public AlumnoResponse findAlumnoResponseById(long id) {
        return  alumnoMapper.toDto(findEntityById(id));
    }

}


