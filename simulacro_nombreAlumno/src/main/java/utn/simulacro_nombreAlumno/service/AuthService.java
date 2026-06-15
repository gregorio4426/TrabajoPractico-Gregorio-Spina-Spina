package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Rol;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.model.request.RegisterAlumnoRequest;
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

    private void validarEmailDisponible(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese email");
        }
    }

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
}