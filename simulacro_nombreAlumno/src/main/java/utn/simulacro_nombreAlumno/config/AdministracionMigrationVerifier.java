package utn.simulacro_nombreAlumno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Order(Ordered.HIGHEST_PRECEDENCE+3)
@ConditionalOnProperty(name="spring.jpa.hibernate.ddl-auto",havingValue="validate")
public class AdministracionMigrationVerifier implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public void run(ApplicationArguments args) {
        try {
            Long marker=jdbc.queryForObject("SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num=5",Long.class);
            Long incoherentes=jdbc.queryForObject("SELECT COUNT(*) FROM usuarios u LEFT JOIN alumnos a ON a.usuario_id=u.id LEFT JOIN profesores p ON p.usuario_id=u.id WHERE (u.rol='ALUMNO' AND a.id IS NULL) OR (u.rol='PROFESOR' AND p.id IS NULL)",Long.class);
            if(!Long.valueOf(1).equals(marker)||!Long.valueOf(0).equals(incoherentes))throw new IllegalStateException("Migración 005 pendiente o perfiles inconsistentes");
        }catch(org.springframework.dao.DataAccessException ex){throw new IllegalStateException("No se pudo verificar la migración 005",ex);}
    }
}
