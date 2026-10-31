package utn.simulacro_nombreAlumno.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.service.UsuarioEstadoService;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import utn.simulacro_nombreAlumno.security.JwtService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired AlumnoRepository alumnos;
    @Autowired ProfesorRepository profesores;
    @Autowired EjercicioRepository ejercicios;
    @Autowired RutinaRepository rutinas;
    @Autowired SesionRepository sesiones;
    @Autowired SerieRepository series;
    @Autowired AsignacionRutinaRepository asignaciones;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    private String teacher, student, next, admin;
    private Alumno alumno;
    private Profesor profesor, nuevo;
    private Long rutinaId, v1Id, dia1, prescripcion1, asignacion1, sesion1;
    private Ejercicio ejercicio;
    private LocalDate fecha;

    @BeforeEach
    void fixture() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Usuario profUser = user("p-" + suffix, Rol.PROFESOR);
        Usuario nextUser = user("n-" + suffix, Rol.PROFESOR);
        Usuario studentUser = user("a-" + suffix, Rol.ALUMNO);
        Usuario adminUser = user("admin-" + suffix, Rol.ADMIN);
        profesor = profesores.save(Profesor.builder().nombre("Ana").apellido("Paz").especialidad("Fuerza")
                .email(profUser.getEmail()).usuario(profUser).build());
        nuevo = profesores.save(Profesor.builder().nombre("Nuevo").apellido("Profesor").especialidad("Fuerza")
                .email(nextUser.getEmail()).usuario(nextUser).build());
        alumno = alumnos.save(Alumno.builder().nombre("Luis").apellido("Rios").edad(25).email(studentUser.getEmail())
                .nivel(Nivel.INTERMEDIO).objetivo(Objetivo.MEJORAR_RESISTENCIA).usuario(studentUser)
                .profesores(new ArrayList<>(List.of(profesor))).build());
        teacher = token(profUser); student = token(studentUser); next = token(nextUser); admin = token(adminUser);
        ejercicio = ejercicios.save(Ejercicio.builder().nombre("Press original").descripcion("Descripción original")
                .grupoMuscular(GrupoMuscular.PECHO).build());
        JsonNode rutina = response(post("/api/rutinas"), teacher, definition("Plan original", 8, ejercicio.getId()), 201);
        rutinaId = rutina.get("id").asLong(); v1Id = rutina.get("rutinaVersionId").asLong();
        dia1 = rutina.at("/dias/0/id").asLong(); prescripcion1 = rutina.at("/dias/0/ejercicios/0/id").asLong();
        JsonNode asignacion = response(post(assignPath()), teacher, null, 200);
        asignacion1 = asignacion.get("id").asLong();
        fecha = LocalDate.now();
        sesion1 = start(dia1).get("id").asLong();
        response(post("/api/sesiones/" + sesion1 + "/series"), student,
                "{\"diaEjercicioId\":" + prescripcion1 + ",\"numeroSerie\":1,\"repsHechas\":7,\"pesoUsado\":52.5}", 201);
        response(put("/api/sesiones/" + sesion1 + "/completar"), student, null, 200);
    }


    @Autowired org.springframework.core.env.Environment environment;
    @Autowired UsuarioEstadoService estadoService;
    @Autowired EstadoSeguridadRepository estado;

    private String email(Rol rol) { return rol==Rol.ADMIN ? jwt.extractUsername(admin) : rol==Rol.PROFESOR ? profesor.getEmail() : alumno.getEmail(); }
    private String roleToken(Rol rol) { return rol==Rol.ADMIN ? admin : rol==Rol.PROFESOR ? teacher : student; }
    private String loginBody(String email,String password) throws Exception { return json.writeValueAsString(java.util.Map.of("email",email,"password",password)); }

    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Rol.class)
    void singleLoginWorksForAllRolesAndNormalizesEmail(Rol role) throws Exception {
        JsonNode result=response(post("/api/auth/login"),null,loginBody(email(role).toUpperCase(java.util.Locale.ROOT),"TestPassword123!"),200);
        assertThat(result.get("rol").asText()).isEqualTo(role.name());
        assertThat(result.get("email").asText()).isEqualTo(email(role));
        assertThat(result.get("token").asText()).isNotBlank();
    }

    @Test void wrongPasswordAndUnknownUserHaveSameSafe401Response() throws Exception {
        JsonNode bad = response(post("/api/auth/login"),null,loginBody(alumno.getEmail(),"Incorrecta"),401);
        JsonNode missing = response(post("/api/auth/login"),null,loginBody("missing-"+UUID.randomUUID()+"@test.com","Incorrecta"),401);
        assertThat(bad.get("mensaje")).isEqualTo(missing.get("mensaje"));
        assertThat(bad.toString()).doesNotContain("password","SQL","exception","trace");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(alumno.getEmail(),"Incorrecta")))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate","Bearer"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"Bearer ","Basic invalid","Bearer broken.token.parts","Bearer null",""})
    void malformedAuthorizationReturns401(String value) throws Exception {
        mvc.perform(get("/api/alumnos/me").header("Authorization",value)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists()).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test void expiredManipulatedAndIncompleteTokensAreRejected() throws Exception {
        java.util.List<String> invalid = new ArrayList<>();
        var now=java.time.Instant.now();
        invalid.add(signed(builder().setExpiration(java.util.Date.from(now.minusSeconds(10)))));
        invalid.add(signed(builder().setExpiration(null)));
        invalid.add(signed(builder().setIssuedAt(null)));
        invalid.add(signed(builder().setIssuedAt(java.util.Date.from(now.plusSeconds(60)))));
        invalid.add(signed(builder().setIssuer("otro-emisor")));
        invalid.add(signed(builder().claim("uid",null)));
        invalid.add(signed(builder().claim("uid",1.5)));
        invalid.add(signed(builder().claim("ver",99)));
        invalid.add(signed(builder().claim("ver",0.5)));
        invalid.add(signed(builder().setSubject(profesor.getEmail())));
        byte[] otherKey=new byte[32];new java.security.SecureRandom().nextBytes(otherKey);
        invalid.add(builder().signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(otherKey),io.jsonwebtoken.SignatureAlgorithm.HS256).compact());
        invalid.add(student.substring(0,student.length()-4)+"xxxx");
        invalid.add(io.jsonwebtoken.Jwts.builder().setSubject(alumno.getEmail()).compact());
        for(String token:invalid) response(get("/api/alumnos/me"),token,null,401);
    }

    @Test void jwtRoleClaimsNeverGrantPrivilegesBeyondDatabaseRole() throws Exception {
        String altered=signed(builder().claim("roles",java.util.List.of(java.util.Map.of("authority","ROLE_ADMIN"))));
        response(get("/api/alumnos/me"),altered,null,200);
        response(get("/api/alumnos"),altered,null,403);
        mvc.perform(get("/api/alumnos/me")).andExpect(status().isUnauthorized());
    }

    @Test void disabledUserCannotLoginOrUseOldTokensEvenAfterReactivation() throws Exception {
        estado.save(new EstadoSeguridad(1L));
        var adminAuth=authentication(admin);
        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);
            estadoService.cambiarEstado(alumno.getUsuario().getId(),false,adminAuth);
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        response(get("/api/alumnos/me"),student,null,401);
        response(post("/api/auth/login"),null,loginBody(alumno.getEmail(),"TestPassword123!"),401);
        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);
            estadoService.cambiarEstado(alumno.getUsuario().getId(),true,adminAuth);
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        response(get("/api/alumnos/me"),student,null,401);
        JsonNode login=response(post("/api/auth/login"),null,loginBody(alumno.getEmail(),"TestPassword123!"),200);
        response(get("/api/alumnos/me"),login.get("token").asText(),null,200);
    }

    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Rol.class)
    void emailChangeIsAtomicAcrossProfileAndIdentityAndRevokesPreviousTokens(Rol role) throws Exception {
        String old=email(role), newEmail="updated-"+UUID.randomUUID()+"@test.com";
        JsonNode changed=response(put("/api/auth/me/email"),roleToken(role),json.writeValueAsString(java.util.Map.of("email",newEmail.toUpperCase(java.util.Locale.ROOT),"passwordActual","TestPassword123!")),200);
        assertThat(changed.get("email").asText()).isEqualTo(newEmail);
        String fresh=changed.get("token").asText();
        response(get("/api/rutinas"),roleToken(role),null,401);
        response(get("/api/rutinas"),fresh,null,200);
        response(post("/api/auth/login"),null,loginBody(old,"TestPassword123!"),401);
        response(post("/api/auth/login"),null,loginBody(newEmail,"TestPassword123!"),200);
        if(role!=Rol.ADMIN) {
            JsonNode profile=response(get(role==Rol.ALUMNO?"/api/alumnos/me":"/api/profesores/me"),fresh,null,200);
            assertThat(profile.get("email").asText()).isEqualTo(newEmail);
        }
    }

    @Test void oldEmailReuseCannotMakeOldJwtAuthenticateAsNewAccount() throws Exception {
        String old=alumno.getEmail();
        response(put("/api/auth/me/email"),student,json.writeValueAsString(java.util.Map.of("email","new-"+UUID.randomUUID()+"@test.com","passwordActual","TestPassword123!")),200);
        String registration=json.writeValueAsString(utn.simulacro_nombreAlumno.model.request.RegisterAlumnoRequest.builder()
                .email(old).password("NewPassword123!").nombre("Nueva").apellido("Persona").edad(25)
                .nivel(Nivel.PRINCIPIANTE).objetivo(Objetivo.MEJORAR_RESISTENCIA).build());
        JsonNode created=response(post("/api/auth/register/alumno"),null,registration,201);
        assertThat(created.get("perfilId").asLong()).isNotEqualTo(alumno.getId());
        response(get("/api/alumnos/me"),student,null,401);
    }

    @Test void emailConflictAndWrongConfirmationDoNotPartiallyChangeIdentity() throws Exception {
        response(put("/api/auth/me/email"),student,json.writeValueAsString(java.util.Map.of("email",profesor.getEmail(),"passwordActual","TestPassword123!")),409);
        response(put("/api/auth/me/email"),student,json.writeValueAsString(java.util.Map.of("email","new-"+UUID.randomUUID()+"@test.com","passwordActual","Incorrecta")),401);
        assertThat(response(get("/api/alumnos/me"),student,null,200).get("email").asText()).isEqualTo(alumno.getEmail());
    }

    @Test void profileContactRemainsEditableButCannotSilentlyChangeLoginEmail() throws Exception {
        String base="{\"nombre\":\"Nombre nuevo\",\"apellido\":\"Apellido nuevo\",\"nivel\":\"INTERMEDIO\",\"objetivo\":\"MEJORAR_RESISTENCIA\",\"emailContacto\":\"contacto@test.com\"";
        response(put("/api/alumnos/me"),student,base+",\"email\":\"otro@test.com\"}",400);
        JsonNode profile=response(put("/api/alumnos/me"),student,base+"}",200);
        assertThat(profile.get("nombre").asText()).isEqualTo("Nombre nuevo");
        assertThat(profile.get("apellido").asText()).isEqualTo("Apellido nuevo");
        assertThat(profile.get("emailContacto").asText()).isEqualTo("contacto@test.com");
        assertThat(profile.get("email").asText()).isEqualTo(alumno.getEmail());
    }

    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Rol.class)
    void passwordChangeRevokesExistingTokensAndKeepsRoles(Rol role) throws Exception {
        JsonNode changed=response(put("/api/auth/me/password"),roleToken(role),"{\"passwordActual\":\"TestPassword123!\",\"nuevaPassword\":\"NewPassword123!\"}",200);
        assertThat(changed.get("rol").asText()).isEqualTo(role.name());
        response(get("/api/rutinas"),roleToken(role),null,401);
        response(get("/api/rutinas"),changed.get("token").asText(),null,200);
        response(post("/api/auth/login"),null,loginBody(email(role),"TestPassword123!"),401);
        response(post("/api/auth/login"),null,loginBody(email(role),"NewPassword123!"),200);
    }

    @Test void corsAllowsConfiguredAngularOriginWithoutOpeningPermissions() throws Exception {
        mvc.perform(options("/api/alumnos/me").header("Origin","http://localhost:4200").header("Access-Control-Request-Method","GET")
                .header("Access-Control-Request-Headers","authorization,content-type")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(as(get("/api/alumnos/me"),student).header("Origin","http://localhost:4200")).andExpect(status().isOk());
        mvc.perform(as(get("/api/alumnos"),student).header("Origin","http://localhost:4200")).andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(options("/api/alumnos/me").header("Origin","https://evil.test").header("Access-Control-Request-Method","GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void errorsUseAppropriateStatusAndNeverExposeInternalDetails() throws Exception {
        response(get("/api/alumnos/me"),null,null,401);
        response(get("/api/alumnos"),student,null,403);
        JsonNode bad=response(get("/api/alumnos/not-a-number"),admin,null,400);
        JsonNode missing=response(get("/api/no-such-resource"),admin,null,404);
        response(get("/api/alumnos/99999999"),admin,null,404);
        assertThat(bad.toString()+missing.toString()).doesNotContain("java.","Exception","trace","SQL","jdbc:");
    }


    @Test void containerErrorDispatchDoesNotLeakInternalsOrBecomeAnAuthenticationFailure() throws Exception {
        mvc.perform(get("/error").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return request; })
                .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE,500)
                .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION,new IllegalStateException("sensitive SQL password")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist()).andExpect(jsonPath("$.exception").doesNotExist());
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
    }

    private io.jsonwebtoken.JwtBuilder builder() {
        var claims=jwt.parseClaims(student);
        return io.jsonwebtoken.Jwts.builder().setSubject(alumno.getEmail()).setIssuer(environment.getProperty("security.jwt.issuer","gimnasio-api"))
                .claim("uid",alumno.getUsuario().getId()).claim("ver",claims.get("ver"))
                .setIssuedAt(new java.util.Date()).setExpiration(java.util.Date.from(java.time.Instant.now().plusSeconds(3600)));
    }
    private String signed(io.jsonwebtoken.JwtBuilder builder) {
        var key=io.jsonwebtoken.security.Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(environment.getProperty("security.jwt.secret-base64")));
        return builder.signWith(key,io.jsonwebtoken.SignatureAlgorithm.HS256).compact();
    }
    private org.springframework.security.core.Authentication authentication(String token) {
        var user=usuarios.findById(jwt.userId(jwt.parseClaims(token))).orElseThrow();var details=new CustomUserDetails(user);
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(details,null,details.getAuthorities());
    }

    private JsonNode start(long day) throws Exception { return response(post("/api/sesiones"), student, startBody(day), 201); }
    private String startBody(long day) { return "{\"diaRutinaId\":" + day + ",\"fecha\":\"" + fecha + "\"}"; }
    private String routinePath() { return "/api/rutinas/" + rutinaId; }
    private String assignPath() { return "/api/rutinas/asignar/alumno/" + alumno.getId() + "/rutina/" + rutinaId; }
    private String definition(String name, int reps, long exerciseId) {
        return "{\"nombre\":\"" + name + "\",\"descripcion\":\"Descripción\",\"dias\":[{\"nombre\":\"Día 1\",\"numeroOrden\":1,\"ejercicios\":[{\"ejercicioId\":" + exerciseId + ",\"seriesSugeridas\":3,\"repsSugeridas\":" + reps + ",\"pesoSugerido\":50.0}]}]}";
    }
    private Usuario user(String name, Rol rol) {
        return usuarios.save(Usuario.builder().email(name + "@test.com").rol(rol).password(passwords.encode("TestPassword123!")).build());
    }
    private String token(Usuario user) { return jwt.generateToken(new CustomUserDetails(user)); }
    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String token) { return request.header("Authorization", "Bearer " + token); }
    private JsonNode response(MockHttpServletRequestBuilder request, String token, String body, int status) throws Exception {
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform((token == null ? request : as(request, token))).andExpect(status().is(status)).andReturn();
        String content = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return content.isEmpty() ? json.nullNode() : json.readTree(content);
    }
}
