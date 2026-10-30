package utn.simulacro_nombreAlumno.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties { private List<String> allowedOrigins = new ArrayList<>(); }
