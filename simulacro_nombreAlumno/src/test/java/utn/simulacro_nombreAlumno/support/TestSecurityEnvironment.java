package utn.simulacro_nombreAlumno.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;
import java.util.*;

/** Solo en classpath de tests: sin claves JWT de prueba fijas reutilizables en producción. */
public class TestSecurityEnvironment implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        env.getPropertySources().addFirst(new MapPropertySource("test-security-defaults", Map.of(
                "security.jwt.secret-base64", Base64.getEncoder().encodeToString(key),
                "security.bootstrap.enabled", false,
                "security.cors.allowed-origins", "http://localhost:4200")));
    }
}
