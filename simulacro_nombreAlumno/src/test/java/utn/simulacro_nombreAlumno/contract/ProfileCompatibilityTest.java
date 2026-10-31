package utn.simulacro_nombreAlumno.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import utn.simulacro_nombreAlumno.mapper.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import utn.simulacro_nombreAlumno.service.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileCompatibilityTest {
    private final AlumnoRepository alumnoRepo = mock(AlumnoRepository.class);
    private final ProfesorRepository profesorRepo = mock(ProfesorRepository.class);
    private final utn.simulacro_nombreAlumno.security.AccesoService acceso = new utn.simulacro_nombreAlumno.security.AccesoService(alumnoRepo);
    private final IdentidadService identidad = mock(IdentidadService.class);
    private final ProfesorService profesores = new ProfesorService(Mappers.getMapper(ProfesorMapper.class), profesorRepo, acceso, identidad);
    private final AlumnoService alumnos = new AlumnoService(Mappers.getMapper(AlumnoMapper.class), alumnoRepo, profesores, acceso, identidad);
    private Usuario usuario;
    private Alumno alumno;
    private Profesor profesor;
    private Authentication auth;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder().id(1L).email("login@test.com").password("encoded").rol(Rol.ALUMNO).build();
        alumno = Alumno.builder().id(8L).nombre("Antes").apellido("Viejo").email("antes@test.com")
                .fechaNacimiento(LocalDate.of(2000, 1, 1)).edad(26).usuario(usuario).build();
        profesor = Profesor.builder().id(7L).nombre("Antes").apellido("Viejo").email("antes@test.com")
                .especialidad("Antes").usuario(usuario).build();
        usuario.setAlumno(alumno);
        auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(usuario), null, java.util.List.of());
        alumno.setEmail(usuario.getEmail());
        profesor.setEmail(usuario.getEmail());
        when(identidad.correoPerfil(any(), any())).thenAnswer(i -> ((Usuario)i.getArgument(0)).getEmail());
        when(alumnoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profesorRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(alumnoRepo.findById(8L)).thenReturn(Optional.of(alumno));
        when(profesorRepo.findById(7L)).thenReturn(Optional.of(profesor));
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void alumnoMeUpdatesAllProfileFieldsButDoesNotChangeLoginIdentity() {
        var request = alumnoRequest();
        var result = alumnos.updateMiPerfil(request, auth);
        assertThat(result.getId()).isEqualTo(8L);
        assertThat(result.getNombre()).isEqualTo("Nuevo");
        assertThat(result.getApellido()).isEqualTo("Apellido");
        assertThat(result.getEmail()).isEqualTo("login@test.com");
        assertThat(result.getEmailContacto()).isEqualTo("contacto@test.com");
        assertThat(result.getFechaNacimiento()).isEqualTo(request.getFechaDeNacimiento());
        assertThat(result.getEdad()).isEqualTo(Period.between(request.getFechaDeNacimiento(), LocalDate.now()).getYears());
        assertThat(result.getNivel()).isEqualTo(Nivel.AVANZADO);
        assertThat(result.getObjetivo()).isEqualTo(Objetivo.MEJORAR_RESISTENCIA);
        assertThat(usuario.getEmail()).isEqualTo("login@test.com");
        assertThat(usuario.getPassword()).isEqualTo("encoded");
        assertThat(usuario.getRol()).isEqualTo(Rol.ALUMNO);
        verify(alumnoRepo).save(alumno);
    }

    @Test
    void alumnoIdAndMeHaveSameUpdateSemantics() {
        var result = alumnos.updateAlumno(8L, alumnoRequest(), auth);
        assertThat(result.getApellido()).isEqualTo("Apellido");
        assertThat(result.getFechaNacimiento()).isEqualTo(LocalDate.of(1999, 5, 3));
        assertThat(result.getEmail()).isEqualTo("login@test.com");
        assertThat(result.getEmailContacto()).isEqualTo("contacto@test.com");
    }

    @Test
    void alumnoCannotUpdateAnotherStudentsId() {
        assertThatThrownBy(() -> alumnos.updateAlumno(99L, alumnoRequest(), auth))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(alumnoRepo, never()).save(any());
    }

    @Test
    void optionalBirthDateAndEmailArePreservedWhenOmitted() {
        var request = alumnoRequest();
        request.setFechaDeNacimiento(null);
        request.setEmail(null);
        var result = alumnos.updateMiPerfil(request, auth);
        assertThat(result.getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(result.getEmail()).isEqualTo("login@test.com");
    }

    @Test
    void legacyProfileWithoutBirthDateDoesNotFail() {
        alumno.setFechaNacimiento(null);
        var request = alumnoRequest();
        request.setFechaDeNacimiento(null);
        assertThat(alumnos.updateMiPerfil(request, auth).getEdad()).isEqualTo(26);
    }

    @Test
    void profesorMeAndIdUpdateContactEmailWithoutChangingLogin() {
        usuario.setProfesor(profesor);
        usuario.setAlumno(null);
        usuario.setRol(Rol.PROFESOR);
        var request = ProfesorRequest.builder().nombre("Nueva").apellido("Paz")
                .emailContacto("contacto@test.com").especialidad("Fuerza").build();
        var result = profesores.updateMiPerfil(request, auth);
        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getNombre()).isEqualTo("Nueva");
        assertThat(result.getApellido()).isEqualTo("Paz");
        assertThat(result.getEmail()).isEqualTo("login@test.com");
        assertThat(result.getEmailContacto()).isEqualTo("contacto@test.com");
        assertThat(result.getEspecialidad()).isEqualTo("Fuerza");
        assertThat(usuario.getEmail()).isEqualTo("login@test.com");
        request.setEmail(null);
        assertThat(profesores.updateProfesor(7L, request).getEmail()).isEqualTo("login@test.com");
        verify(profesorRepo, times(2)).save(profesor);
    }

    private AlumnoRequest alumnoRequest() {
        return AlumnoRequest.builder().nombre("Nuevo").apellido("Apellido").emailContacto("contacto@test.com")
                .FechaDeNacimiento(LocalDate.of(1999, 5, 3)).nivel(Nivel.AVANZADO)
                .objetivo(Objetivo.MEJORAR_RESISTENCIA).build();
    }
}
