package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto", havingValue = "validate")
@RequiredArgsConstructor
public class ConsistenciaMigrationVerifier implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        try {
            Long applied = jdbc.queryForObject("SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 3", Long.class);
            Long inconsistentes = jdbc.queryForObject("SELECT COUNT(*) FROM series s JOIN dias_ejercicio d ON d.id = s.dia_ejercicio_id WHERE s.ejercicio_id IS NULL OR s.ejercicio_id <> d.ejercicio_id OR (s.legado_exento = 1 AND s.incidencia_legado IS NULL)", Long.class);
            if (!Long.valueOf(1).equals(applied) || !Long.valueOf(0).equals(inconsistentes)) throw new IllegalStateException("Migración 003 pendiente o inconsistente");
        } catch (org.springframework.dao.DataAccessException ex) {
            throw new IllegalStateException("No se pudo verificar la migración 003", ex);
        }
    }
}
