package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.*;

@Service @RequiredArgsConstructor
public class BootstrapAdminService {
    private final UsuarioRepository usuarios;
    private final EstadoSeguridadRepository estado;
    private final PasswordEncoder encoder;
    private final IdentidadService identidad;
    private final jakarta.validation.Validator validator;
    @Transactional
    public void inicializar(BootstrapAdminProperties config) {
        if (!config.isEnabled()) return;
        estado.bloquear().orElseThrow(() -> new IllegalStateException("Completar migración 004 antes del bootstrap"));
        if (usuarios.existsByRol(Rol.ADMIN)) return; // No resetea, reactiva ni duplica administradores.
        String email = EmailNormalizer.normalize(config.getEmail());
        if (email == null || email.length() > 150 || !validator.validateValue(utn.simulacro_nombreAlumno.model.request.RegisterProfesorRequest.class, "email", email).isEmpty()) throw new IllegalStateException("Configurar email de bootstrap válido");
        try { PasswordPolicy.validate(config.getPassword(), 12); identidad.exigirEmailDisponible(email, null); }
        catch (RuntimeException ex) { throw new IllegalStateException("Bootstrap inválido: revisar requisitos de contraseña y disponibilidad del email"); }
        usuarios.saveAndFlush(Usuario.builder().email(email).password(encoder.encode(config.getPassword())).rol(Rol.ADMIN).build());
    }
}
