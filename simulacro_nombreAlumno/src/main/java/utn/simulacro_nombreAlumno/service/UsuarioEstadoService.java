package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.AccesoService;

/** Cambio de estado compartido por las operaciones administrativas. */
@Service @RequiredArgsConstructor
public class UsuarioEstadoService {
    private final UsuarioRepository usuarios;
    private final EstadoSeguridadRepository estado;
    private final IdentidadService identidad;
    private final AccesoService acceso;
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void cambiarEstado(Long id, boolean activo, Authentication auth) {
        Usuario principal = acceso.exigirRol(auth, Rol.ADMIN);
        long version = principal.getTokenVersion();
        estado.bloquear().orElseThrow(() -> new IllegalStateException("Completar migración 004"));
        Usuario actor = identidad.bloquear(principal.getId());
        if (!actor.isActivo() || actor.getRol() != Rol.ADMIN || actor.getTokenVersion() != version)
            throw new org.springframework.security.authentication.BadCredentialsException("Autenticación invalidada");
        if (!usuarios.existsById(id)) throw new RecursoNoEncontradoException("Usuario no encontrado");
        Usuario user = identidad.bloquear(id);
        if (user.isActivo() == activo) return;
        if (!activo && user.getRol() == Rol.ADMIN && usuarios.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new ConflictoEntrenamientoException("No se puede desactivar el último administrador activo");
        }
        user.setActivo(activo); user.setTokenVersion(Math.incrementExact(user.getTokenVersion()));
        usuarios.saveAndFlush(user);
    }
}
