package utn.simulacro_nombreAlumno.contract;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import utn.simulacro_nombreAlumno.mapper.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import utn.simulacro_nombreAlumno.service.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HistoryContractTest {
    @Test
    void bothExistingHistoryServicesReturnAssignmentContextWithoutChangingListShape() {
        var repo = mock(AsignacionRutinaRepository.class);
        var alumnos = mock(AlumnoService.class);
        var accessRepo = mock(AlumnoRepository.class);
        var acceso = new utn.simulacro_nombreAlumno.security.AccesoService(accessRepo);
        var service = new RutinaService(mock(RutinaRepository.class), Mappers.getMapper(RutinaMapper.class),
                mock(EjercicioService.class), mock(ProfesorService.class), repo, alumnos,
                Mappers.getMapper(AsignacionMapper.class), acceso, mock(utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento.class));
        var alumno = Alumno.builder().id(8L).build();
        var usuario = Usuario.builder().rol(Rol.ALUMNO).alumno(alumno).build();
        var auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(usuario), null, List.of());
        var rutina = Rutina.builder().id(10L).nombre("Fuerza").descripcion("Plan").build();
        var inicio = LocalDateTime.of(2026, 9, 1, 10, 0);
        var fin = inicio.plusMonths(1);
        var a = AsignacionRutina.builder().id(20L).alumno(alumno).rutina(rutina)
                .fechaInicio(inicio).fechaFin(fin).activa(false).build();
        when(repo.findByAlumno(alumno)).thenReturn(List.of(a));
        when(alumnos.findEntityById(8L)).thenReturn(alumno);
        var own = service.getHistorialMe(auth);
        var byId = service.getHistorialDeAlumno(8L, auth);
        for (var results : List.of(own, byId)) {
            assertThat(results).hasSize(1);
            var result = results.getFirst();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getNombre()).isEqualTo("Fuerza");
            assertThat(result.getDescripcion()).isEqualTo("Plan");
            assertThat(result.getAsignacionId()).isEqualTo(20L);
            assertThat(result.getFechaInicio()).isEqualTo(inicio);
            assertThat(result.getFechaFin()).isEqualTo(fin);
            assertThat(result.getActiva()).isFalse();
        }
    }

    @Test
    void activeRoutineEndpointsAlsoExposeAssignmentContext() {
        var repo = mock(AsignacionRutinaRepository.class);
        var alumnos = mock(AlumnoService.class);
        var accessRepo = mock(AlumnoRepository.class);
        var acceso = new utn.simulacro_nombreAlumno.security.AccesoService(accessRepo);
        var service = new RutinaService(mock(RutinaRepository.class), Mappers.getMapper(RutinaMapper.class),
                mock(EjercicioService.class), mock(ProfesorService.class), repo, alumnos,
                Mappers.getMapper(AsignacionMapper.class), acceso, mock(utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento.class));
        var alumno = Alumno.builder().id(8L).build();
        var auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(
                Usuario.builder().rol(Rol.ALUMNO).alumno(alumno).build()), null, List.of());
        var asignacion = AsignacionRutina.builder().id(20L).alumno(alumno).activa(true)
                .fechaInicio(LocalDateTime.of(2026, 9, 1, 10, 0))
                .rutina(Rutina.builder().id(10L).nombre("Fuerza").build()).build();
        when(repo.findByAlumnoAndActivaTrue(alumno)).thenReturn(Optional.of(asignacion));
        when(alumnos.findEntityById(8L)).thenReturn(alumno);
        for (var result : List.of(service.getRutinaActivaMe(auth), service.getRutinaActivaDeAlumno(8L, auth))) {
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getAsignacionId()).isEqualTo(20L);
            assertThat(result.getFechaInicio()).isEqualTo(asignacion.getFechaInicio());
            assertThat(result.getFechaFin()).isNull();
            assertThat(result.getActiva()).isTrue();
        }
    }
}
