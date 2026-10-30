package utn.simulacro_nombreAlumno.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.service.BootstrapAdminService;
import java.util.*;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class BootstrapSecurityTest {
    private static final String database="stage5_bootstrap_"+UUID.randomUUID().toString().replace("-","");
    @Autowired BootstrapAdminService service;
    @Autowired UsuarioRepository users;
    @Autowired EstadoSeguridadRepository state;
    @Autowired PasswordEncoder encoder;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        String server=System.getenv("MYSQL_TEST_URL");
        properties.add("spring.jpa.hibernate.ddl-auto",()->"create");
        if(server!=null) {
            try(var c=DriverManager.getConnection(server,user(),password());var s=c.createStatement()) { s.execute("CREATE DATABASE `"+database+"`"); }
            String base=server.substring(0,server.indexOf('/',"jdbc:mysql://".length()));
            properties.add("spring.datasource.url",()->base+"/"+database);
            properties.add("spring.datasource.username",BootstrapSecurityTest::user);
            properties.add("spring.datasource.password",BootstrapSecurityTest::password);
            properties.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");
            properties.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.MySQLDialect");
        } else {
            properties.add("spring.datasource.url",()->"jdbc:h2:mem:"+database);
            properties.add("spring.datasource.username",()->"sa"); properties.add("spring.datasource.password",()->"");
            properties.add("spring.datasource.driver-class-name",()->"org.h2.Driver");
            properties.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.H2Dialect");
        }
    }
    @BeforeEach void reset() { users.deleteAll(); state.save(new EstadoSeguridad(1L)); }
    @AfterAll static void cleanup() throws Exception {
        if(System.getenv("MYSQL_TEST_URL")!=null) try(var c=DriverManager.getConnection(System.getenv("MYSQL_TEST_URL"),user(),password());var s=c.createStatement()) {
            s.execute("DROP DATABASE `"+database+"`");
        }
    }
    private static String user(){return System.getenv().getOrDefault("MYSQL_TEST_USERNAME","root");}
    private static String password(){return System.getenv().getOrDefault("MYSQL_TEST_PASSWORD","");}
    private BootstrapAdminProperties properties(String email) {
        var p=new BootstrapAdminProperties();p.setEnabled(true);p.setEmail(email);p.setPassword(UUID.randomUUID().toString());return p;
    }
    @Test void disabledBootstrapDoesNotCreateDefaultAccounts() {
        service.inicializar(new BootstrapAdminProperties());assertThat(users.count()).isZero();
    }
    @Test void missingOrWeakBootstrapPasswordFailsWithoutCreatingUsers() {
        var p=properties("admin@test.com");p.setPassword(null);
        assertThatThrownBy(()->service.inicializar(p)).isInstanceOf(IllegalStateException.class);
        p.setPassword("short");assertThatThrownBy(()->service.inicializar(p)).isInstanceOf(IllegalStateException.class);
        assertThat(users.count()).isZero();
    }
    @Test void concurrentBootstrapCreatesOnlyOneAdminAndNeverResetsItsCredentials() {
        var a=properties("first@test.com");var b=properties("second@test.com");
        CompletableFuture.allOf(CompletableFuture.runAsync(()->service.inicializar(a)),CompletableFuture.runAsync(()->service.inicializar(b))).join();
        assertThat(users.count()).isEqualTo(1);var admin=users.findAll().getFirst();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);assertThat(admin.isActivo()).isTrue();
        String clear=admin.getEmail().equals(a.getEmail())?a.getPassword():b.getPassword();assertThat(encoder.matches(clear,admin.getPassword())).isTrue();
        String hash=admin.getPassword();service.inicializar(properties(admin.getEmail()));
        assertThat(users.findById(admin.getId()).orElseThrow().getPassword()).isEqualTo(hash);
    }
    @Test void bootstrapCannotPromoteExistingStudent() {
        var student=users.save(Usuario.builder().email("student@test.com").rol(Rol.ALUMNO).password(encoder.encode(UUID.randomUUID().toString())).build());
        assertThatThrownBy(()->service.inicializar(properties(student.getEmail()))).isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(student.getId()).orElseThrow().getRol()).isEqualTo(Rol.ALUMNO);assertThat(users.count()).isEqualTo(1);
    }
    @Test void inactiveAdminIsNeverReactivatedByBootstrap() {
        var admin=users.save(Usuario.builder().email("inactive@test.com").rol(Rol.ADMIN).activo(false).tokenVersion(5).password(encoder.encode(UUID.randomUUID().toString())).build());
        service.inicializar(properties("another@test.com"));assertThat(users.count()).isEqualTo(1);
        assertThat(users.findById(admin.getId()).orElseThrow().isActivo()).isFalse();
    }
}
