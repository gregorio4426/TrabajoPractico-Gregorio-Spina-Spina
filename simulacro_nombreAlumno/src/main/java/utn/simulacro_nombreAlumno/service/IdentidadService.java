package utn.simulacro_nombreAlumno.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.model.response.AuthResponse;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.*;

@Service @RequiredArgsConstructor
public class IdentidadService {
    private final UsuarioRepository usuarios;
    private final AlumnoRepository alumnos;
    private final ProfesorRepository profesores;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AccesoService acceso;
    private final utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento validacion;
    @PersistenceContext private EntityManager em;

    public void exigirEmailDisponible(String email, Long excluirId) {
        String normal = EmailNormalizer.normalize(email);
        var existente = usuarios.findByEmailIgnoreCase(normal);
        if (existente.isPresent() && !existente.get().getId().equals(excluirId)
                || alumnos.countEmailOcupado(normal, excluirId == null ? -1L : excluirId) > 0
                || profesores.countEmailOcupado(normal, excluirId == null ? -1L : excluirId) > 0) {
            throw new ConflictoEntrenamientoException("El email no está disponible");
        }
    }

    @Transactional
    public Usuario bloquear(Long id) {
        Usuario user = usuarios.findById(id).orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    /** Los PUT de perfil no cambian silenciosamente las credenciales de acceso. */
    @Transactional
    public String correoPerfil(Usuario linked, String solicitado) {
        if (linked == null) {
            if (solicitado != null) throw new ReglaNegocioException("Perfil sin cuenta: requiere regularización administrativa");
            return null;
        }
        Usuario user = bloquear(linked.getId());
        if (solicitado != null && !user.getEmail().equals(EmailNormalizer.normalize(solicitado))) {
            throw new ReglaNegocioException("Para cambiar el email de acceso usá /api/auth/me/email");
        }
        return user.getEmail();
    }

    private Usuario confirmar(Authentication auth, String password) {
        Usuario principal = acceso.usuario(auth);
        Usuario user = bloquear(principal.getId());
        if (!user.isActivo() || user.getTokenVersion() != principal.getTokenVersion()
                || password == null || !encoder.matches(password, user.getPassword())) throw new BadCredentialsException("Credenciales inválidas");
        return user;
    }

    @Transactional
    public AuthResponse cambiarEmail(CambioEmailRequest request, Authentication auth) {
        acceso.usuario(auth); validacion.validar(request);
        Usuario user = confirmar(auth, request.getPasswordActual());
        String email = EmailNormalizer.normalize(request.getEmail());
        exigirEmailDisponible(email, user.getId());
        user.setEmail(email); user.setTokenVersion(Math.incrementExact(user.getTokenVersion()));
        if (user.getAlumno() != null) user.getAlumno().setEmail(email);
        if (user.getProfesor() != null) user.getProfesor().setEmail(email);
        usuarios.saveAndFlush(user);
        return respuesta(user);
    }

    @Transactional
    public AuthResponse cambiarPassword(CambioPasswordRequest request, Authentication auth) {
        acceso.usuario(auth); validacion.validar(request);
        Usuario user = confirmar(auth, request.getPasswordActual());
        PasswordPolicy.validate(request.getNuevaPassword(), 8);
        user.setPassword(encoder.encode(request.getNuevaPassword()));
        user.setTokenVersion(Math.incrementExact(user.getTokenVersion()));
        usuarios.saveAndFlush(user);
        return respuesta(user);
    }

    public AuthResponse respuesta(Usuario user) {
        Long perfilId = user.getRol() == Rol.ALUMNO && user.getAlumno() != null ? user.getAlumno().getId()
                : user.getRol() == Rol.PROFESOR && user.getProfesor() != null ? user.getProfesor().getId() : null;
        return AuthResponse.builder().token(jwt.generateToken(new CustomUserDetails(user))).email(user.getEmail())
                .rol(user.getRol().name()).perfilId(perfilId).build();
    }
}
