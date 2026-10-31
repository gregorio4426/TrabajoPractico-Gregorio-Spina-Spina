package utn.simulacro_nombreAlumno.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.service.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountStatePolicyTest {
    @Test void cannotDisableLastActiveAdminAndNonAdminCannotChangeState() {
        var users=mock(UsuarioRepository.class);var guard=mock(EstadoSeguridadRepository.class);var identity=mock(IdentidadService.class);
        var access=new AccesoService(mock(AlumnoRepository.class));
        var service=new UsuarioEstadoService(users,guard,identity,access);
        var admin=Usuario.builder().id(1L).rol(Rol.ADMIN).build();var details=new CustomUserDetails(admin);
        var auth=new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities());
        when(guard.bloquear()).thenReturn(Optional.of(new EstadoSeguridad(1L)));
        when(users.existsById(1L)).thenReturn(true);when(identity.bloquear(1L)).thenReturn(admin);when(users.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);
        assertThatThrownBy(()->service.cambiarEstado(1L,false,auth)).isInstanceOf(utn.simulacro_nombreAlumno.exception.ConflictoEntrenamientoException.class);
        assertThat(admin.isActivo()).isTrue();verify(users,never()).saveAndFlush(any());
        var student=new CustomUserDetails(Usuario.builder().rol(Rol.ALUMNO).build());
        assertThatThrownBy(()->service.cambiarEstado(1L,false,new UsernamePasswordAuthenticationToken(student,null,student.getAuthorities())))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
