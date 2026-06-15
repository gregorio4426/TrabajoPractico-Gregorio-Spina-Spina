package utn.simulacro_nombreAlumno.service;

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


    @Transactional
    public AuthResponse registerAlumno(RegisterAlumnoRequest request) {
        validarEmailDisponible(request.getEmail());

        Usuario usuario = usuarioRepository.save(
                Usuario.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .rol(Rol.ALUMNO)
                        .build()
        );
        Alumno alumno = alumnoService.createAlumno(request, usuario);

        String token = jwtService.generateToken(new CustomUserDetails(usuario));

        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .perfilId(alumno.getId())
                .build();
    }

    @Transactional
    public AuthResponse registerProfesor(RegisterProfesorRequest request) {
        validarEmailDisponible(request.getEmail());

        Usuario usuario = usuarioRepository.save(
                Usuario.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .rol(Rol.PROFESOR)
                        .build()
        );

        Profesor profesor = profesorService.createProfesor(request, usuario);

        String token = jwtService.generateToken(new CustomUserDetails(usuario));

        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .perfilId(profesor.getId())
                .build();
    }
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow();

        Long perfilId = null;
        if (usuario.getRol() == Rol.ALUMNO) {
            perfilId = alumnoService.findByUsuario(usuario)
                    .map(Alumno::getId).orElse(null);
        } else if (usuario.getRol() == Rol.PROFESOR) {
            perfilId = profesorService.findByUsuario(usuario)
                    .map(Profesor::getId).orElse(null);
        }

        String token = jwtService.generateToken(new CustomUserDetails(usuario));

        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .perfilId(perfilId)
                .build();
    }

    private void validarEmailDisponible(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese email");
        }
    }

}