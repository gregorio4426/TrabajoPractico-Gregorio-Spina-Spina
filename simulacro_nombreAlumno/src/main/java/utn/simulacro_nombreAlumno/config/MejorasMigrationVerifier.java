package utn.simulacro_nombreAlumno.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @Order(Ordered.HIGHEST_PRECEDENCE+4)
@ConditionalOnProperty(name="spring.jpa.hibernate.ddl-auto",havingValue="validate")
public class MejorasMigrationVerifier implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 public void run(ApplicationArguments args) {
  try {
   Long marker=jdbc.queryForObject("SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num=7",Long.class);
   if(!Long.valueOf(1).equals(marker))throw new IllegalStateException("Migración 007 pendiente: aplicar mejoras funcionales antes de iniciar");
  }catch(org.springframework.dao.DataAccessException ex){throw new IllegalStateException("No se pudo verificar la migración 007",ex);}
 }
}
