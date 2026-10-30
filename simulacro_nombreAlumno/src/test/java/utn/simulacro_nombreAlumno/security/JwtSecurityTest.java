package utn.simulacro_nombreAlumno.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import utn.simulacro_nombreAlumno.model.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSecurityTest {
    private JwtProperties properties() {
        var p = new JwtProperties(); byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        p.setSecretBase64(Base64.getEncoder().encodeToString(key)); return p;
    }
    @Test void missingAndShortSecretsFailWithoutPrintingValue() {
        var p = properties(); p.setSecretBase64(null);
        assertThatThrownBy(() -> new JwtService(p, Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
        p.setSecretBase64("invalid-sensitive-value");
        assertThatThrownBy(() -> new JwtService(p, Clock.systemUTC())).isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-value");
        p.setSecretBase64(Base64.getEncoder().encodeToString(new byte[12]));
        assertThatThrownBy(() -> new JwtService(p, Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
    }
    @Test void expirationAndRevocationAreVerifiedDeterministically() {
        var p = properties(); var time = Instant.parse("2026-10-30T12:00:00Z"); p.setTtl(Duration.ofSeconds(10));
        var jwt = new JwtService(p, Clock.fixed(time, ZoneOffset.UTC));
        var user = Usuario.builder().id(1L).email("a@test.com").rol(Rol.ALUMNO).build();
        var details = new CustomUserDetails(user); String token = jwt.generateToken(details);
        assertThat(jwt.isTokenValid(token,details)).isTrue();
        user.setTokenVersion(1); assertThat(jwt.isTokenValid(token,details)).isFalse();
        user.setTokenVersion(0); user.setActivo(false); assertThat(jwt.isTokenValid(token,details)).isFalse();
        var later = new JwtService(p, Clock.fixed(time.plusSeconds(11), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.parseClaims(token)).isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
    @Test void reservedClaimsCannotBeOverriddenByExtraClaims() {
        var p = properties(); var service = new JwtService(p,Clock.systemUTC());
        var user = new CustomUserDetails(Usuario.builder().id(7L).email("p@test.com").rol(Rol.PROFESOR).tokenVersion(3).build());
        var claims = service.parseClaims(service.generateToken(Map.of("uid",1,"ver",99,"sub","otro@test.com","roles","ROLE_ADMIN"),user));
        assertThat(service.userId(claims)).isEqualTo(7);
        assertThat(claims.getSubject()).isEqualTo("p@test.com");
        assertThat(claims.get("roles").toString()).contains("PROFESOR").doesNotContain("ADMIN");
        assertThat(service.isTokenValid(claims,user)).isTrue();
    }
    @ParameterizedTest @ValueSource(longs={0,-1,86401})
    void invalidLifetimeFails(long seconds) {
        var p=properties(); p.setTtl(Duration.ofSeconds(seconds));
        assertThatThrownBy(() -> new JwtService(p,Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
    }
    @Test void corsRejectsWildcardAndAllowsOnlyExplicitOriginsWithoutCredentials() {
        var props = new CorsProperties(); props.setAllowedOrigins(List.of("*"));
        var config = new SecurityConfig(mock(JwtAuthenticationFilter.class),mock(CustomUserDetailsService.class),mock(SecurityErrorWriter.class),props);
        assertThatThrownBy(config::corsConfigurationSource).isInstanceOf(IllegalStateException.class);
        props.setAllowedOrigins(List.of("http://localhost:4200"));
        var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/api/alumnos/me");
        var cors=config.corsConfigurationSource().getCorsConfiguration(request);
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:4200");
    }
    @Test void internalExceptionsAreNotExposedInResponses() {
        var handler = new utn.simulacro_nombreAlumno.exception.GlobalExceptionHandler();
        var response = handler.handleGenericException(new RuntimeException("SQL jdbc://server password internal.Class"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMensaje()).doesNotContain("SQL","jdbc", "password","Class");
    }
}
