package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import utn.simulacro_nombreAlumno.model.Rol;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("admin@gimnasio.com").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .email("admin@gimnasio.com")
                    .password(passwordEncoder.encode("Admin1234!"))
                    .rol(Rol.ADMIN)
                    .build();
            usuarioRepository.save(admin);
            System.out.println("✅ Admin creado: admin@gimnasio.com / Admin1234!");
        }
    }
}