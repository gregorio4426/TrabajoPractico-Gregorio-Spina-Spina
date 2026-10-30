package utn.simulacro_nombreAlumno.history;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import utn.simulacro_nombreAlumno.model.*;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** Motor real opcional: usa un servidor de TEST y crea/elimina únicamente su propia base aleatoria. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql:.*")
class MysqlMigrationTest {
    @Test
    void migratesLegacyDataWithoutInventingAmbiguousAssignmentsAndCanResumePartialDdl() throws Exception {
        String url = System.getenv("MYSQL_TEST_URL");
        String username = System.getenv().getOrDefault("MYSQL_TEST_USERNAME", "root");
        String password = System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "");
        String database = "stage3_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            execute(connection, "CREATE DATABASE `" + database + "`");
            try {
                connection.setCatalog(database);
                script(connection, "/db/mysql/001_esquema_anterior.sql");
                seed(connection);
                long sessions = number(connection, "SELECT COUNT(*) FROM sesiones");
                long series = number(connection, "SELECT COUNT(*) FROM series");
                long assignments = number(connection, "SELECT COUNT(*) FROM asignaciones_rutina");
                String results = text(connection, "SELECT CONCAT(reps_hechas, ':', peso_usado, ':', s.fecha) FROM series se JOIN sesiones s ON s.id = se.sesion_id WHERE se.id = 1");
                // Simula una ejecución interrumpida después del primer ALTER autocommit.
                execute(connection, "ALTER TABLE rutinas ADD COLUMN rutina_raiz_id BIGINT NULL");
                var verifier = new utn.simulacro_nombreAlumno.config.HistorialMigrationVerifier(
                        new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true)));
                assertThatThrownBy(() -> verifier.run(null)).isInstanceOf(IllegalStateException.class);
                script(connection, "/db/mysql/002_historial_versionado.sql");
                assertThat(number(connection, "SELECT COUNT(*) FROM sesiones")).isEqualTo(sessions);
                assertThat(number(connection, "SELECT COUNT(*) FROM series")).isEqualTo(series);
                assertThat(number(connection, "SELECT COUNT(*) FROM asignaciones_rutina")).isEqualTo(assignments);
                assertThat(text(connection, "SELECT CONCAT(reps_hechas, ':', peso_usado, ':', s.fecha) FROM series se JOIN sesiones s ON s.id = se.sesion_id WHERE se.id = 1")).isEqualTo(results);
                assertThat(number(connection, "SELECT asignacion_id FROM sesiones WHERE id = 1")).isEqualTo(1);
                assertThat(text(connection, "SELECT estado_vinculo_asignacion FROM sesiones WHERE id = 1")).isEqualTo("LEGADO_UNICO_POR_FECHA");
                assertThat(text(connection, "SELECT estado_vinculo_asignacion FROM sesiones WHERE id = 2")).isEqualTo("LEGADO_AMBIGUO");
                assertThat(text(connection, "SELECT estado_vinculo_asignacion FROM sesiones WHERE id = 3")).isEqualTo("LEGADO_SIN_COINCIDENCIA");
                assertThat(text(connection, "SELECT estado_vinculo_asignacion FROM sesiones WHERE id = 4")).isEqualTo("LEGADO_FECHA_IMPRECISA");
                assertThat(number(connection, "SELECT COUNT(*) FROM sesiones WHERE id IN (2,3,4) AND asignacion_id IS NULL")).isEqualTo(3);
                assertThat(number(connection, "SELECT numero_version FROM rutinas WHERE id = 1")).isEqualTo(1);
                assertThat(text(connection, "SELECT origen_version FROM rutinas WHERE id = 1")).isEqualTo("LEGADO_OBSERVADO");
                assertThat(text(connection, "SELECT ejercicio_nombre_snapshot FROM dias_ejercicio WHERE id = 1")).isEqualTo("Press legado");
                assertThat(number(connection, "SELECT COUNT(*) FROM dias_ejercicio WHERE id = 1 AND grupo_muscular_snapshot IS NULL")).isEqualTo(1);
                String captured = text(connection, "SELECT CAST(capturada_en AS CHAR) FROM rutinas WHERE id = 1");
                // Reejecutar no vuelve a capturar datos ni cambia decisiones históricas.
                execute(connection, "UPDATE ejercicios SET nombre = 'Nombre posterior' WHERE id = 1");
                script(connection, "/db/mysql/002_historial_versionado.sql");
                assertThat(text(connection, "SELECT ejercicio_nombre_snapshot FROM dias_ejercicio WHERE id = 1")).isEqualTo("Press legado");
                assertThat(text(connection, "SELECT CAST(capturada_en AS CHAR) FROM rutinas WHERE id = 1")).isEqualTo(captured);
                assertThat(number(connection, "SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 2")).isEqualTo(1);
                assertThatThrownBy(() -> execute(connection, "DELETE FROM asignaciones_rutina WHERE id = 1")).isInstanceOf(SQLException.class);
                assertThatCode(() -> verifier.run(null)).doesNotThrowAnyException();
                script(connection, "/db/mysql/003_consistencia_entrenamientos.sql");
                script(connection, "/db/mysql/004_seguridad_identidad.sql");
                validateHibernate(database, url, username, password);
            } finally {
                connection.setCatalog("mysql");
                execute(connection, "DROP DATABASE `" + database + "`");
            }
        }
    }

    @Test
    void inconsistentLegacyRelationshipsStopMigrationWithoutDeletingRecords() throws Exception {
        String database = "stage3_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(System.getenv("MYSQL_TEST_URL"),
                System.getenv().getOrDefault("MYSQL_TEST_USERNAME", "root"), System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", ""))) {
            execute(connection, "CREATE DATABASE `" + database + "`");
            try {
                connection.setCatalog(database);
                script(connection, "/db/mysql/001_esquema_anterior.sql");
                seed(connection);
                execute(connection, "INSERT INTO dias_rutina(id, nombre, numero_orden, rutina_id) VALUES (2, 'Otro día', 2, 1)");
                execute(connection, "UPDATE dias_ejercicio SET dia_rutina_id = 2 WHERE id = 1");
                assertThatThrownBy(() -> script(connection, "/db/mysql/002_historial_versionado.sql"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("Historial inconsistente");
                assertThat(number(connection, "SELECT COUNT(*) FROM sesiones")).isEqualTo(4);
                assertThat(number(connection, "SELECT COUNT(*) FROM series")).isEqualTo(1);
                assertThat(number(connection, "SELECT COUNT(*) FROM gimnasio_schema_migrations")).isZero();
                // Corrige solo la inconsistencia introducida por este test y reanuda.
                execute(connection, "UPDATE dias_ejercicio SET dia_rutina_id = 1 WHERE id = 1");
                script(connection, "/db/mysql/002_historial_versionado.sql");
                assertThat(number(connection, "SELECT COUNT(*) FROM gimnasio_schema_migrations")).isEqualTo(1);
            } finally {
                connection.setCatalog("mysql");
                execute(connection, "DROP DATABASE `" + database + "`");
            }
        }
    }

    @Test
    void consistencyMigrationPreservesInvalidAndDuplicateLegacySeriesAndAddsConstraints() throws Exception {
        legacyDatabase((connection, database, url, username, password) -> {
            execute(connection, "INSERT INTO series(id, sesion_id, dia_ejercicio_id, numero_serie, reps_hechas, peso_usado, completada) VALUES (2,1,1,1,NULL,-3,1), (3,1,1,0,0,-2,1)");
            String before = text(connection, "SELECT GROUP_CONCAT(CONCAT_WS(':',id,numero_serie,COALESCE(reps_hechas,'NULL'),COALESCE(peso_usado,'NULL')) ORDER BY id) FROM series");
            execute(connection, "ALTER TABLE series ADD COLUMN revision BIGINT NOT NULL DEFAULT 0");
            var verifier = new utn.simulacro_nombreAlumno.config.ConsistenciaMigrationVerifier(new org.springframework.jdbc.core.JdbcTemplate(
                    new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true)));
            assertThatThrownBy(() -> verifier.run(null)).isInstanceOf(IllegalStateException.class);
            script(connection, "/db/mysql/003_consistencia_entrenamientos.sql");
            assertThat(text(connection, "SELECT GROUP_CONCAT(CONCAT_WS(':',id,numero_serie,COALESCE(reps_hechas,'NULL'),COALESCE(peso_usado,'NULL')) ORDER BY id) FROM series")).isEqualTo(before);
            assertThat(number(connection, "SELECT COUNT(*) FROM series WHERE legado_exento = 1")).isEqualTo(3);
            assertThat(text(connection, "SELECT incidencia_legado FROM series WHERE id = 1")).isEqualTo("NUMERO_DUPLICADO");
            assertThat(text(connection, "SELECT incidencia_legado FROM series WHERE id = 2")).contains("VALORES_INVALIDOS", "NUMERO_DUPLICADO");
            assertThat(number(connection, "SELECT COUNT(*) FROM auditoria_entrenamientos")).isZero();
            assertThat(number(connection, "SELECT COUNT(*) FROM sesiones WHERE fecha_corregida IS NULL AND finalizada_en IS NULL")).isEqualTo(4);
            execute(connection, "INSERT INTO series(sesion_id,dia_ejercicio_id,ejercicio_id,numero_serie,reps_hechas,peso_usado,completada) VALUES (1,1,1,2,8,50,1)");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO series(sesion_id,dia_ejercicio_id,ejercicio_id,numero_serie,reps_hechas,completada) VALUES (1,1,1,2,8,1)")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection, "INSERT INTO series(sesion_id,dia_ejercicio_id,ejercicio_id,numero_serie,reps_hechas,peso_usado,completada) VALUES (1,1,1,3,8,-1,1)")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection, "INSERT INTO asignaciones_rutina(alumno_id,rutina_id,activa) VALUES (1,1,1)")).isInstanceOf(SQLException.class);
            script(connection, "/db/mysql/003_consistencia_entrenamientos.sql");
            assertThat(number(connection, "SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 3")).isEqualTo(1);
            assertThat(number(connection, "SELECT COUNT(*) FROM series WHERE legado_exento = 1")).isEqualTo(3);
            assertThatCode(() -> verifier.run(null)).doesNotThrowAnyException();
            script(connection, "/db/mysql/004_seguridad_identidad.sql");
            validateHibernate(database, url, username, password);
        });
    }

    @Test
    void activeAssignmentConflictStopsConsistencyMigrationWithoutChoosingOrDeleting() throws Exception {
        legacyDatabase((connection, database, url, username, password) -> {
            execute(connection, "INSERT INTO asignaciones_rutina(id,alumno_id,rutina_id,activa) VALUES (5,1,1,1)");
            assertThatThrownBy(() -> script(connection, "/db/mysql/003_consistencia_entrenamientos.sql"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("Asignaciones activas duplicadas");
            assertThat(number(connection, "SELECT COUNT(*) FROM asignaciones_rutina")).isEqualTo(5);
            assertThat(number(connection, "SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num = 3")).isZero();
            assertThat(number(connection, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'series' AND column_name = 'revision'")).isZero();
            // Resuelve únicamente la anomalía creada por este test y conserva el registro.
            execute(connection, "UPDATE asignaciones_rutina SET activa = 0 WHERE id = 5");
            script(connection, "/db/mysql/003_consistencia_entrenamientos.sql");
            assertThat(number(connection, "SELECT COUNT(*) FROM asignaciones_rutina")).isEqualTo(5);
        });
    }

    @Test
    void securityMigrationPreservesRolesHashesAndAlternativeContactAndCanResume() throws Exception {
        legacyDatabase((connection,database,url,username,password)-> {
            script(connection,"/db/mysql/003_consistencia_entrenamientos.sql");
            execute(connection,"INSERT INTO usuarios(id,email,password,rol) VALUES (1,' Legacy@Example.test ','stored-hash','ALUMNO')");
            execute(connection,"UPDATE alumnos SET usuario_id=1 WHERE id=1");
            execute(connection,"ALTER TABLE usuarios ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1'");
            long sessions=number(connection,"SELECT COUNT(*) FROM sesiones"), performed=number(connection,"SELECT COUNT(*) FROM series");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            assertThat(text(connection,"SELECT email FROM usuarios WHERE id=1")).isEqualTo("legacy@example.test");
            assertThat(text(connection,"SELECT email FROM alumnos WHERE id=1")).isEqualTo("legacy@example.test");
            assertThat(text(connection,"SELECT email_contacto FROM alumnos WHERE id=1")).isEqualTo("a@test.com");
            assertThat(text(connection,"SELECT password FROM usuarios WHERE id=1")).isEqualTo("stored-hash");
            assertThat(text(connection,"SELECT rol FROM usuarios WHERE id=1")).isEqualTo("ALUMNO");
            assertThat(number(connection,"SELECT COUNT(*) FROM usuarios")).isEqualTo(1);
            assertThat(number(connection,"SELECT COUNT(*) FROM seguridad_sistema WHERE id=1")).isEqualTo(1);
            execute(connection,"UPDATE usuarios SET activo=0,token_version=5 WHERE id=1");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            assertThat(number(connection,"SELECT token_version FROM usuarios WHERE id=1")).isEqualTo(5);
            assertThat(number(connection,"SELECT COUNT(*) FROM usuarios WHERE id=1 AND activo=0")).isEqualTo(1);
            assertThat(number(connection,"SELECT COUNT(*) FROM sesiones")).isEqualTo(sessions);
            assertThat(number(connection,"SELECT COUNT(*) FROM series")).isEqualTo(performed);
            var verifier=new utn.simulacro_nombreAlumno.config.SeguridadMigrationVerifier(new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true)));
            assertThatCode(()->verifier.run(null)).doesNotThrowAnyException();
            validateHibernate(database,url,username,password);
        });
    }

    @Test
    void securityMigrationStopsBeforeDdlOnCanonicalEmailCollision() throws Exception {
        legacyDatabase((connection,database,url,username,password)-> {
            script(connection,"/db/mysql/003_consistencia_entrenamientos.sql");
            execute(connection,"INSERT INTO usuarios(email,password,rol) VALUES ('same@test.com','stored-hash','ALUMNO'),(' same@test.com ','stored-hash','PROFESOR')");
            assertThatThrownBy(()->script(connection,"/db/mysql/004_seguridad_identidad.sql")).isInstanceOf(SQLException.class).hasMessageContaining("Emails de acceso ambiguos");
            assertThat(number(connection,"SELECT COUNT(*) FROM usuarios")).isEqualTo(2);
            assertThat(number(connection,"SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num=4")).isZero();
            assertThat(number(connection,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuarios' AND column_name='activo'")).isZero();
        });
    }

    @Test
    void administrationMigrationPreservesRetainedProfilesHistoryAndAuditOnReplay() throws Exception {
        legacyDatabase((connection,database,url,username,password)-> {
            script(connection,"/db/mysql/003_consistencia_entrenamientos.sql");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            var jdbc=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
            var verifier=new utn.simulacro_nombreAlumno.config.AdministracionMigrationVerifier(jdbc);
            assertThatThrownBy(()->verifier.run(null)).isInstanceOf(IllegalStateException.class);
            script(connection,"/db/mysql/005_administracion.sql");
            execute(connection,"INSERT INTO usuarios(id,email,password,rol) VALUES(10,'a@test.com','stored-hash','ADMIN')");
            execute(connection,"UPDATE alumnos SET usuario_id=10 WHERE id=1");
            execute(connection,"INSERT INTO auditoria_administrativa(actor_id,usuario_id,accion,motivo,antes,despues,creada_en) VALUES(10,10,'CAMBIAR_ROL','Cambio registrado','{}','{}',NOW(6))");
            long sessions=number(connection,"SELECT COUNT(*) FROM sesiones"), performed=number(connection,"SELECT COUNT(*) FROM series");
            script(connection,"/db/mysql/005_administracion.sql");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            assertThat(number(connection,"SELECT COUNT(*) FROM alumnos WHERE usuario_id=10")).isEqualTo(1);
            assertThat(number(connection,"SELECT COUNT(*) FROM auditoria_administrativa")).isEqualTo(1);
            assertThat(number(connection,"SELECT COUNT(*) FROM sesiones")).isEqualTo(sessions);
            assertThat(number(connection,"SELECT COUNT(*) FROM series")).isEqualTo(performed);
            assertThat(text(connection,"SELECT password FROM usuarios WHERE id=10")).isEqualTo("stored-hash");
            assertThatCode(()->verifier.run(null)).doesNotThrowAnyException();
            assertThatCode(()->new utn.simulacro_nombreAlumno.config.SeguridadMigrationVerifier(jdbc).run(null)).doesNotThrowAnyException();
            validateHibernate(database,url,username,password);
        });
    }
    @Test
    void administrationMigrationStopsOnDuplicateLinksAndResumesPartialDdlWithoutInventingAudit() throws Exception {
        legacyDatabase((connection,database,url,username,password)-> {
            script(connection,"/db/mysql/003_consistencia_entrenamientos.sql");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            execute(connection,"INSERT INTO alumnos_profesore(alumno_id,profesor_id) VALUES(1,1),(1,1)");
            assertThatThrownBy(()->script(connection,"/db/mysql/005_administracion.sql")).isInstanceOf(SQLException.class).hasMessageContaining("Vinculaciones duplicadas");
            assertThat(number(connection,"SELECT COUNT(*) FROM alumnos_profesore WHERE alumno_id=1 AND profesor_id=1")).isEqualTo(2);
            assertThat(number(connection,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='auditoria_administrativa'")).isZero();
            // Resolución exclusivamente de la anomalía sintética de este test.
            execute(connection,"DELETE FROM alumnos_profesore WHERE alumno_id=1 AND profesor_id=1 LIMIT 1");
            script(connection,"/db/mysql/005_administracion.sql");
            execute(connection,"DELETE FROM gimnasio_schema_migrations WHERE version_num=5");
            script(connection,"/db/mysql/005_administracion.sql");
            assertThat(number(connection,"SELECT COUNT(*) FROM auditoria_administrativa")).isZero();
            assertThat(number(connection,"SELECT COUNT(*) FROM gimnasio_schema_migrations WHERE version_num=5")).isEqualTo(1);
            assertThatThrownBy(()->execute(connection,"INSERT INTO alumnos_profesore(alumno_id,profesor_id) VALUES(1,1)")).isInstanceOf(SQLException.class);
        });
    }

    @Test
    void administrationMigrationDoesNotInventMissingRoleProfiles() throws Exception {
        legacyDatabase((connection,database,url,username,password)-> {
            script(connection,"/db/mysql/003_consistencia_entrenamientos.sql");
            script(connection,"/db/mysql/004_seguridad_identidad.sql");
            execute(connection,"INSERT INTO usuarios(id,email,password,rol) VALUES(10,'missing-profile@test.com','stored-hash','ALUMNO')");
            assertThatThrownBy(()->script(connection,"/db/mysql/005_administracion.sql")).isInstanceOf(SQLException.class).hasMessageContaining("Cuenta sin perfil");
            assertThat(number(connection,"SELECT COUNT(*) FROM alumnos WHERE usuario_id=10")).isZero();
            assertThat(number(connection,"SELECT COUNT(*) FROM usuarios WHERE id=10")).isEqualTo(1);
            assertThat(number(connection,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='auditoria_administrativa'")).isZero();
        });
    }

    @FunctionalInterface
    interface LegacyAction { void run(Connection connection, String database, String url, String username, String password) throws Exception; }
    private void legacyDatabase(LegacyAction action) throws Exception {
        String database = "stage4_migration_" + UUID.randomUUID().toString().replace("-", "");
        String url = System.getenv("MYSQL_TEST_URL");
        String username = System.getenv().getOrDefault("MYSQL_TEST_USERNAME", "root");
        String password = System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            execute(connection, "CREATE DATABASE `" + database + "`");
            try {
                connection.setCatalog(database);
                script(connection, "/db/mysql/001_esquema_anterior.sql"); seed(connection);
                script(connection, "/db/mysql/002_historial_versionado.sql");
                action.run(connection, database, url, username, password);
            } finally {
                connection.setCatalog("mysql"); execute(connection, "DROP DATABASE `" + database + "`");
            }
        }
    }

    private void seed(Connection c) throws SQLException {
        execute(c, "INSERT INTO profesores(id, nombre, apellido, especialidad, email) VALUES (1, 'Ana', 'Paz', 'Fuerza', 'p@test.com')");
        execute(c, "INSERT INTO alumnos(id, nombre, apellido, edad, email) VALUES (1, 'Luis', 'Rios', 25, 'a@test.com')");
        execute(c, "INSERT INTO alumnos(id, nombre, apellido, edad, email) VALUES (2, 'Otra', 'Persona', 25, 'otra@test.com')");
        execute(c, "INSERT INTO rutinas(id, nombre, descripcion, profesor_id) VALUES (1, 'Plan legado', 'Descripción', 1), (2, 'Otra rutina', 'Descripción', 1)");
        execute(c, "INSERT INTO ejercicios(id, nombre, descripcion) VALUES (1, 'Press legado', 'Descripción')");
        execute(c, "INSERT INTO dias_rutina(id, nombre, numero_orden, rutina_id) VALUES (1, 'Día 1', 1, 1)");
        execute(c, "INSERT INTO dias_ejercicio(id, dia_rutina_id, ejercicio_id, series_sugeridas, reps_sugeridas) VALUES (1, 1, 1, 3, 8)");
        execute(c, "INSERT INTO asignaciones_rutina(id, alumno_id, rutina_id, fecha_inicio, fecha_fin, activa) VALUES (1, 1, 1, '2026-01-01 12:00:00', '2026-02-01', 0), (2, 1, 1, '2026-02-01', '2026-03-01', 0), (3, 2, 1, '2026-01-01', NULL, 1), (4, 1, 2, '2026-01-01', NULL, 1)");
        // El día del cambio tiene dos candidatas; una fecha fuera del rango no tiene ninguna.
        execute(c, "INSERT INTO sesiones(id, alumno_id, dia_rutina_id, fecha, completada) VALUES (1, 1, 1, '2026-01-15', 1), (2, 1, 1, '2026-02-01', 1), (3, 1, 1, '2025-12-01', 1), (4, 1, 1, '2026-01-01', 1)");
        execute(c, "INSERT INTO series(id, sesion_id, dia_ejercicio_id, numero_serie, reps_hechas, peso_usado, completada) VALUES (1, 1, 1, 1, 7, 52.5, 1)");
    }

    private void validateHibernate(String database, String serverUrl, String user, String password) throws Exception {
        try(var connection=DriverManager.getConnection(serverUrl,user,password)) {
            connection.setCatalog(database); script(connection,"/db/mysql/005_administracion.sql");
        }
        String base = serverUrl.substring(0, serverUrl.indexOf('/', "jdbc:mysql://".length()));
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", base + "/" + database)
                .applySetting("hibernate.connection.username", user).applySetting("hibernate.connection.password", password)
                .applySetting("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver")
                .applySetting("hibernate.hbm2ddl.auto", "validate")
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy").build();
        try {
            var metadata = new MetadataSources(registry);
            for (Class<?> entity : new Class<?>[]{Usuario.class, Profesor.class, Alumno.class, Rutina.class, DiaRutina.class,
                    DiaEjercicio.class, Ejercicio.class, AsignacionRutina.class, Sesion.class, Serie.class, AuditoriaEntrenamiento.class, EstadoSeguridad.class, AuditoriaAdministrativa.class}) metadata.addAnnotatedClass(entity);
            try (var factory = metadata.buildMetadata().buildSessionFactory()) {
                assertThat(factory.isOpen()).isTrue();
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }

    private void script(Connection c, String path) throws Exception {
        String source;
        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException(path);
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String delimiter = ";";
        StringBuilder statement = new StringBuilder();
        for (String line : source.lines().toList()) {
            String stripped = line.strip();
            if (stripped.startsWith("--") || stripped.isEmpty()) continue;
            if (stripped.startsWith("DELIMITER ")) { delimiter = stripped.substring(10).trim(); continue; }
            statement.append(line).append('\n');
            if (stripped.endsWith(delimiter)) {
                String sql = statement.toString().strip();
                execute(c, sql.substring(0, sql.length() - delimiter.length()));
                statement.setLength(0);
            }
        }
        if (!statement.toString().isBlank()) throw new IllegalArgumentException("SQL incompleto: " + path);
    }
    private void execute(Connection c, String sql) throws SQLException { try (Statement s = c.createStatement()) { s.execute(sql); } }
    private long number(Connection c, String sql) throws SQLException { try (Statement s = c.createStatement(); var r = s.executeQuery(sql)) { r.next(); return r.getLong(1); } }
    private String text(Connection c, String sql) throws SQLException { try (Statement s = c.createStatement(); var r = s.executeQuery(sql)) { r.next(); return r.getString(1); } }
}
