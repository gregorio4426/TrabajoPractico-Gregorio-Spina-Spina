package utn.simulacro_nombreAlumno.security;

import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, BootstrapAdminProperties.class, CorsProperties.class})
public class SecurityPropertiesConfig {
    @Bean Clock securityClock() { return Clock.systemUTC(); }
}
