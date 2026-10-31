package utn.simulacro_nombreAlumno.service;

import utn.simulacro_nombreAlumno.security.AccesoService;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.ProfesorMapper;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.ProfesorRepository;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProfesorService  {

    private final ProfesorMapper profesorMapper;
    private final ProfesorRepository profesorRepository;
    private final AccesoService acceso;
    private final IdentidadService identidad;

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
        acceso.usuario(SecurityContextHolder.getContext().getAuthentication());
        return profesorMapper.toLISTDto(profesorRepository.findAll());
    }

    // SCRUM-26 — Modificar perfil profesor
    @Transactional
    public ProfesorResponse updateProfesor(Long id, ProfesorRequest nuevo) {
        acceso.editarPerfilProfesor(id, SecurityContextHolder.getContext().getAuthentication());
        Profesor profesor = findEntityById(id);
        aplicarPerfil(profesor, nuevo);
        profesorRepository.save(profesor);
        return profesorMapper.toDto(profesor);
    }

    public Profesor findEntityById(long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor no encontrado"));
    }

    public ProfesorResponse findProfesorResponseById(long id) {
        acceso.usuario(SecurityContextHolder.getContext().getAuthentication());
        return profesorMapper.toDto(findEntityById(id));
    }


    public java.util.Optional<Profesor> findByUsuario(utn.simulacro_nombreAlumno.model.Usuario usuario) {
        return profesorRepository.findByUsuario(usuario);
    }

    public ProfesorResponse getMiPerfil(org.springframework.security.core.Authentication authentication) {
        Profesor profesor = findEntityById(acceso.profesorPropio(authentication).getId());
        return profesorMapper.toDto(profesor);
    }

    @Transactional
    public ProfesorResponse updateMiPerfil(ProfesorRequest request, Authentication authentication) {
        Profesor profesor = findEntityById(acceso.profesorPropio(authentication).getId());
        aplicarPerfil(profesor, request);
        profesorRepository.save(profesor);
        return profesorMapper.toDto(profesor);
    }
    private void aplicarPerfil(Profesor profesor, ProfesorRequest request) {
        String email = identidad.correoPerfil(profesor.getUsuario(), request.getEmail());
        profesor.setNombre(request.getNombre());
        profesor.setApellido(request.getApellido());
        if (email != null) profesor.setEmail(email);
        if (request.getEmailContacto() != null) profesor.setEmailContacto(request.getEmailContacto());
        profesor.setEspecialidad(request.getEspecialidad());
    }
}


