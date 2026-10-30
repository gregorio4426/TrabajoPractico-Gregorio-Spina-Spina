package utn.simulacro_nombreAlumno.service;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Rol;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.model.request.LoginRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterAlumnoRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.AuthResponse;
import utn.simulacro_nombreAlumno.repository.UsuarioRepository;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import utn.simulacro_nombreAlumno.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoService alumnoService;
    private final ProfesorService profesorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final IdentidadService identidad;
    private final utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento validacion;


    @Transactional
    public AuthResponse registerAlumno(RegisterAlumnoRequest request) {
        request.setEmail(utn.simulacro_nombreAlumno.security.EmailNormalizer.normalize(request.getEmail()));
        validacion.validar(request);
        utn.simulacro_nombreAlumno.security.PasswordPolicy.validate(request.getPassword(), 8);
        identidad.exigirEmailDisponible(request.getEmail(), null);

        Usuario usuario = usuarioRepository.save(
                Usuario.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .rol(Rol.ALUMNO)
                        .build()
        );
        Alumno alumno = alumnoService.createAlumno(request, usuario);

        usuario.setAlumno(alumno);
        return identidad.respuesta(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AuthResponse registerProfesor(RegisterProfesorRequest request) {
        request.setEmail(utn.simulacro_nombreAlumno.security.EmailNormalizer.normalize(request.getEmail()));
        validacion.validar(request);
        utn.simulacro_nombreAlumno.security.PasswordPolicy.validate(request.getPassword(), 8);
        identidad.exigirEmailDisponible(request.getEmail(), null);

        Usuario usuario = usuarioRepository.save(
                Usuario.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .rol(Rol.PROFESOR)
                        .build()
        );

        Profesor profesor = profesorService.createProfesor(request, usuario);

        usuario.setProfesor(profesor);
        return identidad.respuesta(usuario);
    }
    @Transactional
    public AuthResponse login(LoginRequest request) {
        request.setEmail(utn.simulacro_nombreAlumno.security.EmailNormalizer.normalize(request.getEmail()));
        validacion.validar(request);
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        Usuario authenticated = ((CustomUserDetails)auth.getPrincipal()).getUsuario();
        Usuario usuario = identidad.bloquear(authenticated.getId());
        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas");
        }
        return identidad.respuesta(usuario);
    }
}
