package utn.simulacro_nombreAlumno.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccesoServiceTest {
    private final AlumnoRepository repo = mock(AlumnoRepository.class);
    private final AccesoService access = new AccesoService(repo);

    @ParameterizedTest
    @EnumSource(Rol.class)
    void explicitReadAndWriteMatrix(Rol role) {
        var auth = auth(role);
        when(repo.existsByIdAndProfesores_Id(8L, 7L)).thenReturn(true);
        assertThatCode(() -> access.leerAlumno(8L, auth)).doesNotThrowAnyException();
        if (role == Rol.ALUMNO) {
            assertThatThrownBy(() -> access.gestionarAlumno(8L, auth)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> access.leerAlumno(99L, auth)).isInstanceOf(AccessDeniedException.class);
            assertThatCode(() -> access.editarPerfilAlumno(8L, auth)).doesNotThrowAnyException();
        } else {
            assertThatCode(() -> access.gestionarAlumno(8L, auth)).doesNotThrowAnyException();
            if (role == Rol.PROFESOR) {
                assertThatThrownBy(() -> access.editarPerfilAlumno(8L, auth)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> access.editarPerfilProfesor(99L, auth)).isInstanceOf(AccessDeniedException.class);
            } else {
                assertThatCode(() -> access.editarPerfilAlumno(99L, auth)).doesNotThrowAnyException();
                assertThatCode(() -> access.editarPerfilProfesor(99L, auth)).doesNotThrowAnyException();
            }
        }
    }

    @Test
    void revocationIsReadFromDatabaseEvenWithSameAuthenticationObject() {
        var auth = auth(Rol.PROFESOR);
        when(repo.existsByIdAndProfesores_Id(8L, 7L)).thenReturn(true, false);
        access.leerAlumno(8L, auth);
        assertThatThrownBy(() -> access.leerAlumno(8L, auth)).isInstanceOf(AccessDeniedException.class);
        verify(repo, times(2)).existsByIdAndProfesores_Id(8L, 7L);
    }

    @Test
    void missingAuthenticationOrProfileFailsClosed() {
        assertThatThrownBy(() -> access.leerAlumno(8L, null)).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        var missing = new UsernamePasswordAuthenticationToken(new CustomUserDetails(Usuario.builder().rol(Rol.PROFESOR).build()), null, List.of());
        assertThatThrownBy(() -> access.profesorPropio(missing)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.leerAlumno(8L, missing)).isInstanceOf(AccessDeniedException.class);
        var anonymous = new UsernamePasswordAuthenticationToken("anonymous", null, List.of());
        assertThatThrownBy(() -> access.leerAlumno(8L, anonymous)).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        var unauthenticated = new UsernamePasswordAuthenticationToken(new CustomUserDetails(Usuario.builder().rol(Rol.ADMIN).build()), null);
        assertThatThrownBy(() -> access.leerAlumno(8L, unauthenticated)).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Test
    void unrelatedTeacherCannotReadOrManageEvenWhenStudentExists() {
        var auth = auth(Rol.PROFESOR);
        assertThatThrownBy(() -> access.leerAlumno(8L, auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.gestionarAlumno(8L, auth)).isInstanceOf(AccessDeniedException.class);
    }

    private Authentication auth(Rol rol) {
        var user = Usuario.builder().rol(rol).alumno(rol == Rol.ALUMNO ? Alumno.builder().id(8L).build() : null)
                .profesor(rol == Rol.PROFESOR ? Profesor.builder().id(7L).build() : null).build();
        var principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
