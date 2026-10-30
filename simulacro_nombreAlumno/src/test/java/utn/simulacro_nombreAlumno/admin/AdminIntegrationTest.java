package utn.simulacro_nombreAlumno.admin;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AdminIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios; @Autowired AlumnoRepository alumnos;
    @Autowired ProfesorRepository profesores; @Autowired EjercicioRepository ejercicios;
    @Autowired RutinaRepository rutinas; @Autowired AsignacionRutinaRepository asignaciones;
    @Autowired SesionRepository sesiones; @Autowired SerieRepository series;
    @Autowired EstadoSeguridadRepository estado; @Autowired AuditoriaAdministrativaRepository auditorias;
    @Autowired JwtService jwt; @Autowired PasswordEncoder encoder;
    @Autowired AdminConsultaService consulta; @Autowired AdminGestionService gestion;
    Usuario au,pu,nu,su;Profesor profesor,nuevo;Alumno alumno;
    String admin,teacher,next,student,suffix;long routine,day,prescription,assignment,session;
    @BeforeEach void fixture() throws Exception {
        suffix=UUID.randomUUID().toString();estado.save(new EstadoSeguridad(1L));
        au=user("admin",Rol.ADMIN);pu=user("prof",Rol.PROFESOR);nu=user("next",Rol.PROFESOR);su=user("student",Rol.ALUMNO);
        profesor=profesores.save(Profesor.builder().nombre("Ana").apellido("Paz").especialidad("Fuerza").email(pu.getEmail()).usuario(pu).build());
        nuevo=profesores.save(Profesor.builder().nombre("Bea").apellido("Rios").especialidad("Resistencia").email(nu.getEmail()).usuario(nu).build());
        alumno=alumnos.save(Alumno.builder().nombre("Luis").apellido("Rios").edad(25).email(su.getEmail()).usuario(su).nivel(Nivel.INTERMEDIO).objetivo(Objetivo.MEJORAR_RESISTENCIA).profesores(new ArrayList<>(List.of(profesor))).build());
        teacher=token(pu);next=token(nu);student=token(su);admin=token(au);
        var exercise=ejercicios.save(Ejercicio.builder().nombre("Press").descripcion("Press histórico").grupoMuscular(GrupoMuscular.PECHO).build());
        String definition="{\"nombre\":\"Plan original\",\"descripcion\":\"Plan\",\"dias\":[{\"nombre\":\"Día\",\"numeroOrden\":1,\"ejercicios\":[{\"ejercicioId\":"+exercise.getId()+",\"seriesSugeridas\":2,\"repsSugeridas\":8,\"pesoSugerido\":40}]}]}";
        var r=call(post("/api/rutinas"),teacher,definition,201);routine=r.get("id").asLong();day=r.at("/dias/0/id").asLong();prescription=r.at("/dias/0/ejercicios/0/id").asLong();
        assignment=call(post("/api/rutinas/asignar/alumno/"+alumno.getId()+"/rutina/"+routine),teacher,null,200).get("id").asLong();
        session=call(post("/api/sesiones"),student,"{\"diaRutinaId\":"+day+",\"fecha\":\""+LocalDate.now()+"\"}",201).get("id").asLong();
        call(post("/api/sesiones/"+session+"/series"),student,"{\"diaEjercicioId\":"+prescription+",\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":42.5}",201);
        call(put("/api/sesiones/"+session+"/completar"),student,null,200);
    }
    @Test void entireAdminNamespaceRequiresAdminBeforeValidationOrExistenceChecks() throws Exception {
        for(String token:List.of(student,teacher)) {
            for(String path:List.of("usuarios","usuarios/9999999","alumnos","alumnos/9999999","profesores","profesores/9999999","rutinas","rutinas/9999999","rutinas/9999999/versiones","asignaciones","asignaciones/9999999","sesiones","sesiones/9999999","sesiones/9999999/auditoria","resumen","auditoria","alumnos/9999999/historial","alumnos/9999999/sesiones","alumnos/9999999/profesores","profesores/9999999/alumnos")) call(get("/api/admin/"+path),token,null,403);
            for(String path:List.of("usuarios","alumnos","profesores","alumnos/9999999/cuenta","profesores/9999999/cuenta"))call(post("/api/admin/"+path),token,"{}",403);
            for(String path:List.of("usuarios/9999999/estado","usuarios/9999999/rol","usuarios/9999999/email","alumnos/9999999","profesores/9999999","alumnos/9999999/profesores/9999999","alumnos/9999999/profesores/reemplazo"))call(put("/api/admin/"+path),token,"{}",403);
            call(delete("/api/admin/alumnos/9999999/profesores/9999999"),token,"{}",403);
        }
        call(get("/api/admin/usuarios"),null,null,401);
        call(post("/api/admin/usuarios"),null,"{}",401);
    }
    @Test void servicesAlsoEnforceAdminWithoutRelyingOnControllerOrAuthenticationArgument() {
        var d=new CustomUserDetails(su);var a=new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities());SecurityContextHolder.getContext().setAuthentication(a);
        try {
            assertThatThrownBy(()->consulta.usuarios(new AdminFiltro(),a)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->gestion.crear(new AdminDtos.CrearUsuario("u@test.com","Password123!",Rol.ADMIN,null,null),a)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }finally{SecurityContextHolder.clearContext();}
    }
    @ParameterizedTest @EnumSource(Rol.class)
    void adminCreatesAllRolesWithCoherentProfilesAndNoSensitiveResponse(Rol role) throws Exception {
        String email="created-"+role+"-"+suffix+"@test.com";
        var body=json.createObjectNode().put("email",email).put("password","InitialPassword123!").put("rol",role.name());
        if(role==Rol.ALUMNO)body.set("alumno",json.readTree(alumnoDatos()));if(role==Rol.PROFESOR)body.set("profesor",json.readTree(profesorDatos()));
        var created=call(post("/api/admin/usuarios"),admin,body.toString(),201);
        assertThat(created.get("email").asText()).isEqualTo(email.toLowerCase(Locale.ROOT));assertSafe(created);
        assertThat(created.get("activo").asBoolean()).isTrue();assertThat(created.get("revision").asLong()).isZero();
        var login=call(post("/api/auth/login"),null,"{\"email\":\""+email+"\",\"password\":\"InitialPassword123!\"}",200);
        assertThat(login.get("rol").asText()).isEqualTo(role.name());
        if(role!=Rol.ADMIN)assertThat(login.get("perfilId").asLong()).isEqualTo(created.get(role==Rol.ALUMNO?"alumnoId":"profesorId").asLong());
        call(post("/api/admin/usuarios"),admin,body.toString(),409);
        assertThat(call(get("/api/admin/auditoria").param("usuarioId",created.get("id").asText()),admin,null,200).get("totalElementos").asLong()).isEqualTo(1);
    }
    @Test void profileCreationAndEditingAreAtomicAndValidated() throws Exception {
        var a=json.createObjectNode().put("email","profile-"+suffix+"@test.com").put("password","InitialPassword123!");a.set("perfil",json.readTree(alumnoDatos()));
        var created=call(post("/api/admin/alumnos"),admin,a.toString(),201);assertThat(created.at("/cuenta/rol").asText()).isEqualTo("ALUMNO");
        var edited=call(put("/api/admin/alumnos/"+created.at("/perfil/id").asLong()),admin,alumnoDatos().replace("Luis","Luca"),200);
        assertThat(edited.at("/perfil/nombre").asText()).isEqualTo("Luca");assertThat(edited.at("/perfil/email")).isEqualTo(created.at("/cuenta/email"));
        a.put("email","prof-profile-"+suffix+"@test.com");a.set("perfil",json.readTree(profesorDatos()));
        var p=call(post("/api/admin/profesores"),admin,a.toString(),201);
        var changed=call(put("/api/admin/profesores/"+p.at("/perfil/id").asLong()),admin,profesorDatos().replace("Fuerza","Movilidad"),200);
        assertThat(changed.at("/perfil/especialidad").asText()).isEqualTo("Movilidad");
        long count=usuarios.count();a.put("email","bad-"+suffix+"@test.com");a.set("perfil",json.readTree(alumnoDatos().replace("\"edad\":25","\"edad\":0")));
        call(post("/api/admin/alumnos"),admin,a.toString(),400);assertThat(usuarios.count()).isEqualTo(count);
        a.set("perfil",json.readTree(alumnoDatos().replace("\"edad\":25","\"edad\":25,\"fechaNacimiento\":\"2020-01-01\"")));
        call(post("/api/admin/alumnos"),admin,a.toString(),400);assertThat(usuarios.count()).isEqualTo(count);
        a.set("perfil",json.readTree(profesorDatos()));a.put("password","short");call(post("/api/admin/profesores"),admin,a.toString(),400);
    }
    @Test void listsSearchFilterAndPaginateWithoutReturningCredentials() throws Exception {
        var users=call(get("/api/admin/usuarios").param("q",suffix).param("rol","PROFESOR").param("activo","true").param("tamanio","1").param("orden","email").param("direccion","desc"),admin,null,200);
        assertThat(users.get("totalElementos").asLong()).isEqualTo(2);assertThat(users.get("contenido").size()).isEqualTo(1);assertThat(users.get("totalPaginas").asInt()).isEqualTo(2);assertSafe(users);
        assertThat(call(get("/api/admin/usuarios").param("q",suffix).param("rol","PROFESOR").param("tamanio","1").param("pagina","1"),admin,null,200).get("contenido").size()).isEqualTo(1);
        var a=call(get("/api/admin/alumnos").param("profesorId",profesor.getId().toString()).param("nivel","INTERMEDIO").param("vigente","true"),admin,null,200);
        assertThat(a.get("totalElementos").asLong()).isEqualTo(1);assertThat(a.at("/contenido/0/perfil/id").asLong()).isEqualTo(alumno.getId());
        var p=call(get("/api/admin/profesores").param("alumnoId",alumno.getId().toString()).param("especialidad","FUER"),admin,null,200);assertThat(p.get("totalElementos").asLong()).isEqualTo(1);
        for(String query:List.of("pagina=-1","tamanio=0","tamanio=101","rol=ROOT","orden=password","direccion=sideways","q="+"a".repeat(151)))call(get("/api/admin/usuarios?"+query),admin,null,400);
        assertThat(call(get("/api/admin/usuarios").param("q","%"),admin,null,200).get("totalElementos").asLong()).isZero();
        call(get("/api/admin/usuarios/99999999"),admin,null,404);
    }
    @Test void adminReplacesTeachersAndNewTeacherCanReadAllOldHistory() throws Exception {
        var body="{\"profesorAnteriorId\":"+profesor.getId()+",\"profesorNuevoId\":"+nuevo.getId()+",\"motivo\":\"Cambio de profesor\"}";
        call(put("/api/admin/alumnos/"+alumno.getId()+"/profesores/reemplazo"),admin,body,200);
        call(get("/api/sesiones/"+session),teacher,null,403);var s=call(get("/api/sesiones/"+session),next,null,200);assertThat(s.at("/series/0/pesoUsado").asDouble()).isEqualTo(42.5);
        assertThat(call(get("/api/admin/profesores/"+nuevo.getId()+"/alumnos"),admin,null,200).get("totalElementos").asLong()).isEqualTo(1);
        call(delete(link(nuevo.getId())),admin,"{\"motivo\":\"Fin del vínculo\"}",200);call(get("/api/sesiones/"+session),next,null,403);
        call(put(link(profesor.getId())),admin,"{\"motivo\":\"Nueva vinculación\"}",200);call(get("/api/sesiones/"+session),teacher,null,200);
        call(put(link(profesor.getId())),admin,"{\"motivo\":\"Reintento\"}",200);
        assertThat(call(get("/api/admin/alumnos/"+alumno.getId()+"/profesores"),admin,null,200).get("totalElementos").asLong()).isEqualTo(1);
        assertThat(sesiones.existsById(session)).isTrue();assertThat(asignaciones.existsById(assignment)).isTrue();
    }
    @Test void deactivateRevokeReactivateAndRejectStaleAdministrativeWrites() throws Exception {
        var disabled=call(put(userPath(su)+"/estado"),admin,"{\"activo\":false,\"revision\":0,\"motivo\":\"Baja\"}",200);
        assertThat(disabled.get("revision").asLong()).isEqualTo(1);call(get("/api/alumnos/me"),student,null,401);
        call(put(userPath(su)+"/estado"),admin,"{\"activo\":true,\"revision\":0,\"motivo\":\"Solicitud obsoleta\"}",409);
        call(put(userPath(su)+"/estado"),admin,"{\"activo\":true,\"revision\":1,\"motivo\":\"Reingreso\"}",200);
        call(get("/api/alumnos/me"),student,null,401);var login=login(su.getEmail());call(get("/api/sesiones/"+session),login,null,200);
        assertThat(sesiones.existsById(session)).isTrue();
    }
    @Test void studentRoleRoundTripKeepsProfileAndHistoryButDoesNotRestoreTeacherLinks() throws Exception {
        var switched=call(put(userPath(su)+"/rol"),admin,"{\"rol\":\"PROFESOR\",\"revision\":0,\"motivo\":\"Nuevo puesto\",\"profesor\":"+profesorDatos()+"}",200);
        long profId=switched.get("profesorId").asLong();assertThat(switched.get("alumnoId").asLong()).isEqualTo(alumno.getId());
        var audit=call(get("/api/admin/auditoria").param("usuarioId",su.getId().toString()),admin,null,200);
        var before=json.readTree(audit.at("/contenido/0/antes").asText());
        assertThat(before.get("profesoresAlumno").get(0).asLong()).isEqualTo(profesor.getId());assertSafe(audit);
        call(get("/api/alumnos/me"),student,null,401);String ptoken=login(su.getEmail());call(get("/api/alumnos/me"),ptoken,null,403);call(get("/api/profesores/me"),ptoken,null,200);
        call(get("/api/sesiones/"+session),teacher,null,403);
        assertThat(call(get("/api/admin/alumnos/"+alumno.getId()),admin,null,200).get("perfilVigente").asBoolean()).isFalse();
        call(put(userPath(su)+"/email"),admin,"{\"email\":\"renamed-"+suffix+"@test.com\",\"revision\":1,\"motivo\":\"Actualización\"}",200);
        assertThat(alumnos.findById(alumno.getId()).orElseThrow().getEmail()).isEqualTo(profesores.findById(profId).orElseThrow().getEmail());
        var restored=call(put(userPath(su)+"/rol"),admin,"{\"rol\":\"ALUMNO\",\"revision\":2,\"motivo\":\"Retorno\"}",200);assertThat(restored.get("alumnoId").asLong()).isEqualTo(alumno.getId());
        String stoken=login(restored.get("email").asText());var history=call(get("/api/sesiones/"+session),stoken,null,200);assertThat(history.at("/series/0/pesoUsado").asDouble()).isEqualTo(42.5);
        assertThat(call(get("/api/alumnos/me"),stoken,null,200).get("profesores").size()).isZero();
        call(get("/api/profesores/me"),stoken,null,403);call(get("/api/profesores/me"),ptoken,null,401);
    }
    @Test void retiringTeacherCannotBeRelinkedThroughExistingStudentEndpoint() throws Exception {
        call(put(userPath(pu)+"/rol"),admin,"{\"rol\":\"ADMIN\",\"revision\":0,\"motivo\":\"Cambio de rol\"}",200);
        call(get("/api/profesores/me/alumnos"),teacher,null,401);
        call(put(link(profesor.getId())),admin,"{\"motivo\":\"Vínculo inválido\"}",409);
        call(post("/api/alumnos/me/profesores/"+profesor.getId()),student,null,409);
        assertThat(call(get("/api/admin/rutinas/"+routine),admin,null,200).get("profesorId").asLong()).isEqualTo(profesor.getId());
        call(put(userPath(pu)+"/rol"),admin,"{\"rol\":\"PROFESOR\",\"revision\":1,\"motivo\":\"Regreso\"}",200);
        call(get("/api/sesiones/"+session),login(pu.getEmail()),null,403);
    }
    @Test void roleAndEmailValidationLeaveOriginalIdentityIntact() throws Exception {
        call(put(userPath(su)+"/rol"),admin,"{\"rol\":\"PROFESOR\",\"revision\":0,\"motivo\":\"Sin perfil\"}",400);
        call(put(userPath(su)+"/email"),admin,"{\"email\":\""+pu.getEmail()+"\",\"revision\":0,\"motivo\":\"Colisión\"}",409);
        call(put(userPath(su)+"/rol"),admin,"{\"rol\":\"ROOT\",\"revision\":0,\"motivo\":\"Inválido\"}",400);
        var user=usuarios.findById(su.getId()).orElseThrow();assertThat(user.getRol()).isEqualTo(Rol.ALUMNO);assertThat(user.getTokenVersion()).isZero();
        call(get("/api/alumnos/me"),student,null,200);
    }
    @Test void adminSeesArchivedVersionsAssignmentsSessionsAndRealSummaryWithoutTrainingPrivileges() throws Exception {
        call(put("/api/rutinas/"+routine),teacher,"{\"nombre\":\"Plan editado\",\"descripcion\":\"Actualizado\"}",200);
        call(delete("/api/rutinas/"+routine),teacher,null,204);
        var list=call(get("/api/admin/rutinas").param("profesorId",profesor.getId().toString()).param("archivada","true").param("q","Plan editado"),admin,null,200);
        assertThat(list.get("totalElementos").asLong()).isEqualTo(1);assertThat(list.at("/contenido/0/numeroVersion").asInt()).isEqualTo(2);
        assertThat(call(get("/api/admin/rutinas/"+routine+"/versiones"),admin,null,200).get("totalElementos").asLong()).isEqualTo(2);
        var a=call(get("/api/admin/asignaciones/"+assignment),admin,null,200);assertThat(a.get("numeroVersion").asInt()).isEqualTo(1);
        assertThat(call(get("/api/admin/alumnos/"+alumno.getId()+"/historial"),admin,null,200).get("totalElementos").asLong()).isEqualTo(1);
        assertThat(call(get("/api/admin/asignaciones").param("rutinaId",""+routine).param("alumnoId",alumno.getId().toString()),admin,null,200).get("totalElementos").asLong()).isEqualTo(1);
        var s=call(get("/api/admin/sesiones").param("alumnoId",alumno.getId().toString()).param("rutinaId",""+routine).param("finalizada","true").param("desde",LocalDate.now().toString()).param("hasta",LocalDate.now().toString()),admin,null,200);
        assertThat(s.get("totalElementos").asLong()).isEqualTo(1);assertThat(s.at("/contenido/0/series/0/pesoUsado").asDouble()).isEqualTo(42.5);
        call(get("/api/admin/sesiones/"+session),admin,null,200);call(get("/api/admin/sesiones/"+session+"/auditoria"),admin,null,200);
        var summary=call(get("/api/admin/resumen"),admin,null,200);assertThat(summary.get("usuarios").asLong()).isEqualTo(usuarios.count());assertThat(summary.get("sesiones").asLong()).isEqualTo(sesiones.count());
        call(post("/api/sesiones"),admin,"{\"diaRutinaId\":"+day+",\"fecha\":\""+LocalDate.now()+"\"}",403);
        call(post("/api/rutinas"),admin,"{}",403);
    }
    @Test void legacyProfilesCanGetAnAccountWithoutChangingTheirIdentityOrHistory() throws Exception {
        var a=alumnos.save(Alumno.builder().nombre("Legado").apellido("Rios").edad(25).email("orphan-"+suffix+"@test.com").build());
        var result=call(post("/api/admin/alumnos/"+a.getId()+"/cuenta"),admin,"{\"password\":\"InitialPassword123!\",\"motivo\":\"Regularización\"}",201);
        assertThat(result.get("alumnoId").asLong()).isEqualTo(a.getId());call(get("/api/alumnos/me"),login(a.getEmail()),null,200);
        call(post("/api/admin/alumnos/"+a.getId()+"/cuenta"),admin,"{\"password\":\"InitialPassword123!\",\"motivo\":\"Reintento\"}",409);
        var orphan=profesores.save(Profesor.builder().nombre("Legado").apellido("Paz").especialidad("Fuerza").email("orphan-p-"+suffix+"@test.com").build());
        var p=call(post("/api/admin/profesores/"+orphan.getId()+"/cuenta"),admin,"{\"password\":\"InitialPassword123!\",\"motivo\":\"Regularización\"}",201);assertThat(p.get("profesorId").asLong()).isEqualTo(orphan.getId());
        assertThat(call(get("/api/admin/alumnos").param("sinCuenta","true").param("q",a.getEmail()),admin,null,200).get("totalElementos").asLong()).isZero();
    }
    private void assertSafe(JsonNode n){assertThat(n.toString()).doesNotContain("password","tokenVersion","\"token\"","$2a$","$2b$");}
    private String alumnoDatos(){return "{\"nombre\":\"Luis\",\"apellido\":\"Rios\",\"edad\":25,\"nivel\":\"INTERMEDIO\",\"objetivo\":\"MEJORAR_RESISTENCIA\",\"emailContacto\":\"contacto@test.com\"}";}
    private String profesorDatos(){return "{\"nombre\":\"Ana\",\"apellido\":\"Paz\",\"especialidad\":\"Fuerza\"}";}
    private String userPath(Usuario u){return "/api/admin/usuarios/"+u.getId();}
    private String link(Long id){return "/api/admin/alumnos/"+alumno.getId()+"/profesores/"+id;}
    private Usuario user(String n,Rol r){return usuarios.save(Usuario.builder().email(n+"-"+suffix+"@test.com").rol(r).password(encoder.encode("InitialPassword123!")).build());}
    private String token(Usuario u){return jwt.generateToken(new CustomUserDetails(u));}
    private String login(String email)throws Exception{return call(post("/api/auth/login"),null,"{\"email\":\""+email+"\",\"password\":\"InitialPassword123!\"}",200).get("token").asText();}
    private JsonNode call(MockHttpServletRequestBuilder r,String t,String body,int status)throws Exception{
        if(t!=null)r.header("Authorization","Bearer "+t);if(body!=null)r.contentType(MediaType.APPLICATION_JSON).content(body);
        String result=mvc.perform(r).andExpect(status().is(status)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return result.isEmpty()?json.nullNode():json.readTree(result);
    }
}
