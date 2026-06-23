package utn.simulacro_nombreAlumno.service;

import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
import utn.simulacro_nombreAlumno.security.CustomUserDetails;


import java.time.LocalDate;
import java.time.Period;
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

    public AlumnoResponse updateAlumno(Long id, AlumnoRequest request, Authentication authentication) {
        verificarAcceso(id, authentication);

        Alumno alumno = findEntityById(id);
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEmail(request.getEmail());
        alumno.setFechaNacimiento(request.getFechaDeNacimiento());
        int edad = Period.between(request.getFechaDeNacimiento(), LocalDate.now()).getYears();
        alumno.setEdad(edad);
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
    public AlumnoResponse elegirProfesor(Long alumnoId, Long profesorId ,Authentication authentication) {
        verificarAcceso(alumnoId , authentication);
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

    private void verificarAcceso(Long alumnoId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Usuario usuario = userDetails.getUsuario();
        boolean esProfesor = usuario.getRol() == Rol.PROFESOR;
        boolean esDueño = usuario.getAlumno() != null && usuario.getAlumno().getId().equals(alumnoId);
        if (!esProfesor && !esDueño) {
            throw new org.springframework.security.access.AccessDeniedException("No tenés permiso para modificar este alumno");
        }
    }

    public AlumnoResponse getMiPerfil(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        return alumnoMapper.toDto(alumno);
    }

    public AlumnoResponse updateMiPerfil(AlumnoRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        alumno.setNombre(request.getNombre());
        int edad = Period.between(alumno.getFechaNacimiento(), LocalDate.now()).getYears();
        alumno.setEdad(edad);
        alumno.setNivel(request.getNivel());
        alumno.setObjetivo(request.getObjetivo());
        alumnoRepository.save(alumno);
        return alumnoMapper.toDto(alumno);
    }

    @Transactional
    public AlumnoResponse elegirProfesorMe(Long profesorId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        Profesor profesor = profesorService.findEntityById(profesorId);
        if (!alumno.getProfesores().contains(profesor)) {
            alumno.getProfesores().add(profesor);
        }
        alumnoRepository.save(alumno);
        return alumnoMapper.toDto(alumno);
    }
}


