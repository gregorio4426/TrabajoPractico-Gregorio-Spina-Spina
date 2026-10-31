package utn.simulacro_nombreAlumno.admin;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc
class AdminConcurrencyTest {
    private static final String database="stage6_admin_"+UUID.randomUUID().toString().replace("-","");

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
            properties.add("spring.datasource.username",AdminConcurrencyTest::user);
            properties.add("spring.datasource.password",AdminConcurrencyTest::password);
            properties.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");
            properties.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.MySQLDialect");
        } else {
            properties.add("spring.datasource.url",()->"jdbc:h2:mem:"+database);
            properties.add("spring.datasource.username",()->"sa"); properties.add("spring.datasource.password",()->"");
            properties.add("spring.datasource.driver-class-name",()->"org.h2.Driver");
            properties.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.H2Dialect");
        }
    }

    @AfterAll static void cleanup() throws Exception {
        if(System.getenv("MYSQL_TEST_URL")!=null) try(var c=DriverManager.getConnection(System.getenv("MYSQL_TEST_URL"),user(),password());var s=c.createStatement()) {
            s.execute("DROP DATABASE `"+database+"`");
        }
    }
    private static String user(){return System.getenv().getOrDefault("MYSQL_TEST_USERNAME","root");}
    private static String password(){return System.getenv().getOrDefault("MYSQL_TEST_PASSWORD","");}

    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JwtService jwt;
    @Autowired AlumnoRepository alumnos; @Autowired ProfesorRepository profesores;
    @Autowired AuditoriaAdministrativaRepository auditorias;
    @Autowired AdminGestionService gestion; @Autowired JdbcTemplate jdbc;
    Usuario first,second;
    @BeforeEach void reset() {
        auditorias.deleteAll();alumnos.deleteAll();profesores.deleteAll();users.deleteAll();state.save(new EstadoSeguridad(1L));
        first=user("first",Rol.ADMIN);second=user("second",Rol.ADMIN);
    }
    @Test void lastActiveAdminCannotBeDeactivatedOrDemotedEvenByThemself() throws Exception {
        users.deleteById(second.getId());String token=token(first);
        assertThat(status(put(path(first)+"/estado"),token,"{\"activo\":false,\"revision\":0,\"motivo\":\"Baja\"}")).isEqualTo(409);
        assertThat(status(put(path(first)+"/rol"),token,"{\"rol\":\"ALUMNO\",\"revision\":0,\"motivo\":\"Cambio\",\"alumno\":"+studentData()+"}")).isEqualTo(409);
        assertThat(users.countByRolAndActivoTrue(Rol.ADMIN)).isEqualTo(1);assertThat(auditorias.count()).isZero();
    }
    @Test void concurrentSelfDeactivationAndDemotionCannotRemoveBothAdministrators() throws Exception {
        String a=token(first),b=token(second);var gate=new CountDownLatch(1);
        var disable=CompletableFuture.supplyAsync(()->racing(gate,put(path(first)+"/estado"),a,"{\"activo\":false,\"revision\":0,\"motivo\":\"Baja\"}"));
        var demote=CompletableFuture.supplyAsync(()->racing(gate,put(path(second)+"/rol"),b,"{\"rol\":\"ALUMNO\",\"revision\":0,\"motivo\":\"Cambio\",\"alumno\":"+studentData()+"}"));
        gate.countDown();assertThat(List.of(disable.get(30,TimeUnit.SECONDS),demote.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        assertThat(users.countByRolAndActivoTrue(Rol.ADMIN)).isEqualTo(1);assertThat(auditorias.count()).isEqualTo(1);
    }
    @Test void concurrentRoleUpdatesRejectStaleRevisionAndNeverCreateDuplicateProfiles() throws Exception {
        Usuario target=user("target",Rol.ADMIN);String token=token(first);var gate=new CountDownLatch(1);
        String body="{\"rol\":\"ALUMNO\",\"revision\":0,\"motivo\":\"Cambio\",\"alumno\":"+studentData()+"}";
        var a=CompletableFuture.supplyAsync(()->racing(gate,put(path(target)+"/rol"),token,body));
        var b=CompletableFuture.supplyAsync(()->racing(gate,put(path(target)+"/rol"),token,body));gate.countDown();
        assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        assertThat(alumnos.count()).isEqualTo(1);assertThat(users.findById(target.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
    }
    @Test void failedAuditRollsBackStateAndTokenRevocationTogether() throws Exception {
        jdbc.execute("ALTER TABLE auditoria_administrativa ADD CONSTRAINT ck_test_admin_audit CHECK (accion <> 'CAMBIAR_ESTADO')");
        try {
            assertThat(status(put(path(second)+"/estado"),token(first),"{\"activo\":false,\"revision\":0,\"motivo\":\"Baja\"}")).isEqualTo(409);
            var unchanged=users.findById(second.getId()).orElseThrow();assertThat(unchanged.isActivo()).isTrue();assertThat(unchanged.getTokenVersion()).isZero();
            assertThat(auditorias.count()).isZero();assertThat(status(get("/api/admin/resumen"),token(second),null)).isEqualTo(200);
        }finally{jdbc.execute("ALTER TABLE auditoria_administrativa DROP CONSTRAINT ck_test_admin_audit");}
    }
    @Test void staleAdminPrincipalIsRevalidatedInsideServiceAfterRoleRevocation() throws Exception {
        var details=new CustomUserDetails(second);var auth=new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities());
        assertThat(status(put(path(second)+"/rol"),token(first),"{\"rol\":\"ALUMNO\",\"revision\":0,\"motivo\":\"Cambio\",\"alumno\":"+studentData()+"}")).isEqualTo(200);
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertThatThrownBy(()->gestion.crear(new AdminDtos.CrearUsuario("denied@test.com","InitialPassword123!",Rol.ADMIN,null,null),auth))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        }finally{SecurityContextHolder.clearContext();}
        assertThat(users.findByEmailIgnoreCase("denied@test.com")).isEmpty();
    }
    private int racing(CountDownLatch gate,MockHttpServletRequestBuilder r,String token,String body){try{gate.await(10,TimeUnit.SECONDS);return status(r,token,body);}catch(Exception ex){throw new CompletionException(ex);}}
    private int status(MockHttpServletRequestBuilder r,String token,String body)throws Exception {
        if(token!=null)r.header("Authorization","Bearer "+token);if(body!=null)r.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(r).andReturn().getResponse().getStatus();
    }
    private Usuario user(String n,Rol rol){return users.save(Usuario.builder().email(n+"@test.com").rol(rol).password(encoder.encode("InitialPassword123!")).build());}
    private String token(Usuario u){return jwt.generateToken(new CustomUserDetails(u));}
    private String path(Usuario u){return "/api/admin/usuarios/"+u.getId();}
    private String studentData(){return "{\"nombre\":\"Luis\",\"apellido\":\"Paz\",\"edad\":25,\"nivel\":\"INTERMEDIO\",\"objetivo\":\"MEJORAR_RESISTENCIA\"}";}
}
