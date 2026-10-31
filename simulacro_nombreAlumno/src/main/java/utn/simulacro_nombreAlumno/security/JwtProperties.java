package utn.simulacro_nombreAlumno.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Getter @Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secretBase64;
    private Duration ttl = Duration.ofHours(24);
    private String issuer = "gimnasio-api";
}
