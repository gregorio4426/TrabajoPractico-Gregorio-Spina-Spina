package utn.simulacro_nombreAlumno.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;

import java.util.Arrays;
import java.util.Objects;

/** Consulta la vinculación actual en cada operación; no confía en listas del JWT/principal. */
@Service
@RequiredArgsConstructor
public class AccesoService {
    private final AlumnoRepository alumnos;

    public Usuario usuario(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails details)
                || details.getUsuario() == null || details.getUsuario().getRol() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Autenticación requerida");
        }
        if (!details.isEnabled()) throw new org.springframework.security.authentication.DisabledException("Credenciales inválidas");
        return details.getUsuario();
    }

    public Usuario exigirRol(Authentication auth, Rol... roles) {
        Usuario usuario = usuario(auth);
        if (Arrays.stream(roles).noneMatch(rol -> rol == usuario.getRol())) {
            throw new AccessDeniedException("No tenés permiso para esta operación");
        }
        return usuario;
    }

    public Alumno alumnoPropio(Authentication auth) {
        Alumno alumno = exigirRol(auth, Rol.ALUMNO).getAlumno();
        if (alumno == null || alumno.getId() == null) throw new AccessDeniedException("Perfil de alumno requerido");
        return alumno;
    }

    public Profesor profesorPropio(Authentication auth) {
        Profesor profesor = exigirRol(auth, Rol.PROFESOR).getProfesor();
        if (profesor == null || profesor.getId() == null) throw new AccessDeniedException("Perfil de profesor requerido");
        return profesor;
    }

    public boolean puedeLeerAlumno(Long id, Authentication auth) {
        Usuario usuario = usuario(auth);
        return switch (usuario.getRol()) {
            case ADMIN -> true;
            case ALUMNO -> usuario.getAlumno() != null && id != null && Objects.equals(usuario.getAlumno().getId(), id);
            case PROFESOR -> usuario.getProfesor() != null && usuario.getProfesor().getId() != null && id != null
                    && alumnos.existsByIdAndProfesores_Id(id, usuario.getProfesor().getId());
        };
    }

    public void leerAlumno(Long id, Authentication auth) {
        if (!puedeLeerAlumno(id, auth)) throw new AccessDeniedException("Alumno no vinculado o ajeno");
    }

    public void gestionarAlumno(Long id, Authentication auth) {
        exigirRol(auth, Rol.ADMIN, Rol.PROFESOR);
        leerAlumno(id, auth);
    }

    public void editarPerfilAlumno(Long id, Authentication auth) {
        Usuario usuario = exigirRol(auth, Rol.ADMIN, Rol.ALUMNO);
        if (usuario.getRol() != Rol.ADMIN && !Objects.equals(alumnoPropio(auth).getId(), id)) {
            throw new AccessDeniedException("Solo podés modificar tu propio perfil");
        }
    }

    public void editarPerfilProfesor(Long id, Authentication auth) {
        Usuario usuario = exigirRol(auth, Rol.ADMIN, Rol.PROFESOR);
        if (usuario.getRol() != Rol.ADMIN && !Objects.equals(profesorPropio(auth).getId(), id)) {
            throw new AccessDeniedException("Solo podés modificar tu propio perfil");
        }
    }
}
