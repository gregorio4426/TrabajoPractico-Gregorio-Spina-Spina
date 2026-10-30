package utn.simulacro_nombreAlumno.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "security.bootstrap")
public class BootstrapAdminProperties {
    private boolean enabled;
    private String email;
    private String password;
}
