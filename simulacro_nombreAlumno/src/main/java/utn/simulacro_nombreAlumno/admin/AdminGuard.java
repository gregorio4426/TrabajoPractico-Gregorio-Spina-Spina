package utn.simulacro_nombreAlumno.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.AccesoService;
import utn.simulacro_nombreAlumno.service.IdentidadService;

@Service @RequiredArgsConstructor
public class AdminGuard {
    private final EstadoSeguridadRepository estado;
    private final AccesoService acceso;
    private final IdentidadService identidad;
    /** Orden global -> actor -> destino. Revalida tras esperar, antes de cualquier escritura. */
    @Transactional
    public Usuario bloquear(Authentication auth) {
        Usuario principal = acceso.exigirRol(auth,Rol.ADMIN);
        long version = principal.getTokenVersion();
        estado.bloquear().orElseThrow(() -> new IllegalStateException("Completar migración 004"));
        Usuario actor = identidad.bloquear(principal.getId());
        if (!actor.isActivo() || actor.getRol()!=Rol.ADMIN || actor.getTokenVersion()!=version)
            throw new BadCredentialsException("Autenticación invalidada");
        return actor;
    }
}
