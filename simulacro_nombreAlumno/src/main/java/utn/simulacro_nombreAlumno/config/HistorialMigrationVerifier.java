package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Impide arrancar sobre ALTERs parciales que Hibernate validate por sí solo aceptaría. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto", havingValue = "validate")
@RequiredArgsConstructor
public class HistorialMigrationVerifier implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long aplicada = jdbc.queryForObject("SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 2", Long.class);
            if (!Long.valueOf(1).equals(aplicada)) throw new IllegalStateException("Debe completar la migración 002 antes de iniciar la aplicación");
            Long pendientes = jdbc.queryForObject("SELECT COUNT(*) FROM sesiones WHERE estado_vinculo_asignacion = 'LEGADO_SIN_EVALUAR' OR (estado_vinculo_asignacion = 'REGISTRADA' AND asignacion_id IS NULL)", Long.class);
            Long snapshots = jdbc.queryForObject("SELECT COUNT(*) FROM dias_ejercicio WHERE ejercicio_nombre_snapshot IS NULL", Long.class);
            Long inconsistentes = jdbc.queryForObject("SELECT COUNT(*) FROM sesiones s JOIN asignaciones_rutina a ON a.id = s.asignacion_id JOIN dias_rutina d ON d.id = s.dia_rutina_id WHERE a.alumno_id <> s.alumno_id OR a.rutina_id <> d.rutina_id", Long.class);
            if (!Long.valueOf(0).equals(pendientes) || !Long.valueOf(0).equals(snapshots) || !Long.valueOf(0).equals(inconsistentes)) {
                throw new IllegalStateException("Migración histórica incompleta o referencias inconsistentes; revisar antes de iniciar");
            }
        } catch (DataAccessException ex) {
            throw new IllegalStateException("No se pudo verificar la migración histórica 002; revisar esquema y permisos", ex);
        }
    }
}
