package utn.simulacro_nombreAlumno.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,   "/api/ejercicios"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,    "/api/ejercicios/**"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE, "/api/ejercicios/**"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,   "/api/rutinas"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,    "/api/rutinas/**"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE, "/api/rutinas/**"
                        ).hasRole("PROFESOR")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,   "/api/rutinas/*/asignar/**"
                        ).hasRole("PROFESOR")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }


}
