package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto", havingValue = "validate")
public class SeguridadMigrationVerifier implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        try {
            Long marker = jdbc.queryForObject("SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 4", Long.class);
            Long guard = jdbc.queryForObject("SELECT COUNT(*) FROM seguridad_sistema WHERE id = 1", Long.class);
            Long alumnos = jdbc.queryForObject("SELECT COUNT(*) FROM alumnos p JOIN usuarios u ON u.id = p.usuario_id WHERE BINARY p.email <> BINARY u.email", Long.class);
            Long profesores = jdbc.queryForObject("SELECT COUNT(*) FROM profesores p JOIN usuarios u ON u.id = p.usuario_id WHERE BINARY p.email <> BINARY u.email", Long.class);
            if (!Long.valueOf(1).equals(marker) || !Long.valueOf(1).equals(guard) || !Long.valueOf(0).equals(alumnos) || !Long.valueOf(0).equals(profesores)) {
                throw new IllegalStateException("Migración 004 pendiente o identidades inconsistentes");
            }
        } catch (org.springframework.dao.DataAccessException ex) { throw new IllegalStateException("No se pudo verificar la migración 004", ex); }
    }
}
