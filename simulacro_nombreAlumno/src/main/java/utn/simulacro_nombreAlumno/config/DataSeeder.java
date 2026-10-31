package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import utn.simulacro_nombreAlumno.security.BootstrapAdminProperties;
import utn.simulacro_nombreAlumno.service.BootstrapAdminService;

@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final BootstrapAdminService bootstrap;
    private final BootstrapAdminProperties properties;
    @Override public void run(String... args) { bootstrap.inicializar(properties); }
}
