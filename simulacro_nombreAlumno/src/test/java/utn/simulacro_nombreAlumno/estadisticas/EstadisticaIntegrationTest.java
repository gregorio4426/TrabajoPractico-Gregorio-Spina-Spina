package utn.simulacro_nombreAlumno.estadisticas;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class EstadisticaIntegrationTest {
    private static final String database="stage7_stats_"+UUID.randomUUID().toString().replace("-","");

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
            properties.add("spring.datasource.username",EstadisticaIntegrationTest::user);
            properties.add("spring.datasource.password",EstadisticaIntegrationTest::password);
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

    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired AlumnoRepository alumnos; @Autowired ProfesorRepository profesores;
    @Autowired EjercicioRepository ejercicios; @Autowired SesionRepository sesiones;
    @Autowired SerieRepository series; @Autowired RutinaRepository rutinas;
    @Autowired AsignacionRutinaRepository asignaciones; @Autowired JwtService jwt;
    @Autowired EstadisticaService service;
    @Autowired DiaRutinaRepository dias; @Autowired DiaEjercicioRepository prescripciones;
    Usuario au,pu,nu,su,ou;Profesor profesor,nuevo;Alumno alumno,ajeno,cero;
    String admin,teacher,next,student,other;
    long rutina,day,pressId,squatId,pressPrescription,squatPrescription,assignment,firstSession,secondSession;
    LocalDate today;
    @BeforeEach void fixture() throws Exception {
        for(String table:List.of("auditoria_administrativa","auditoria_entrenamientos","series","sesiones","asignaciones_rutina","dias_ejercicio","dias_rutina"))jdbc.update("DELETE FROM "+table);
        jdbc.update("DELETE FROM rutinas WHERE rutina_raiz_id IS NOT NULL");jdbc.update("DELETE FROM rutinas");
        for(String table:List.of("alumnos_profesore","alumnos","profesores","usuarios","ejercicios"))jdbc.update("DELETE FROM "+table);
        state.save(new EstadoSeguridad(1L));today=LocalDate.now(ZoneOffset.UTC);
        au=user("admin",Rol.ADMIN);pu=user("prof",Rol.PROFESOR);nu=user("next",Rol.PROFESOR);su=user("student",Rol.ALUMNO);ou=user("other",Rol.ALUMNO);
        profesor=profesores.save(Profesor.builder().nombre("Ana").apellido("Paz").especialidad("Fuerza").email(pu.getEmail()).usuario(pu).build());
        nuevo=profesores.save(Profesor.builder().nombre("Bea").apellido("Rios").especialidad("Fuerza").email(nu.getEmail()).usuario(nu).build());
        alumno=alumno(su,"Luis",profesor);ajeno=alumno(ou,"Ajeno",nuevo);cero=alumno(user("zero",Rol.ALUMNO),"Sin registros",profesor);
        admin=token(au);teacher=token(pu);next=token(nu);student=token(su);other=token(ou);
        var press=ejercicios.save(Ejercicio.builder().nombre("Press original").descripcion("Histórico").grupoMuscular(GrupoMuscular.PECHO).build());pressId=press.getId();
        var squat=ejercicios.save(Ejercicio.builder().nombre("Sentadilla").descripcion("Histórico").grupoMuscular(GrupoMuscular.PIERNAS).build());squatId=squat.getId();
        var r=call(post("/api/rutinas"),teacher,definition(),201);rutina=r.get("id").asLong();day=r.at("/dias/0/id").asLong();pressPrescription=r.at("/dias/0/ejercicios/0/id").asLong();squatPrescription=r.at("/dias/0/ejercicios/1/id").asLong();
        assignment=call(post("/api/rutinas/asignar/alumno/"+alumno.getId()+"/rutina/"+rutina),teacher,null,200).get("id").asLong();
        firstSession=start(student,today.minusDays(10));serie(firstSession,student,pressPrescription,1,8,50.0);serie(firstSession,student,pressPrescription,2,10,null);serie(firstSession,student,squatPrescription,1,5,20.0);close(firstSession,student);
        long revision=call(get("/api/sesiones/"+firstSession),student,null,200).get("revision").asLong();
        call(patch("/api/sesiones/"+firstSession+"/correccion"),student,"{\"fecha\":\""+today.minusDays(9)+"\",\"revision\":"+revision+",\"motivo\":\"Fecha corregida\"}",200);
        secondSession=start(student,today.minusDays(8));serie(secondSession,student,pressPrescription,1,10,60.0);serie(secondSession,student,pressPrescription,2,5,0.0);close(secondSession,student);
        long open=start(student,today.minusDays(7));serie(open,student,pressPrescription,1,100,500.0);
        sesiones.save(Sesion.builder().alumno(alumno).diaRutina(dias.findById(day).orElseThrow()).fecha(today.minusDays(6)).completada(true).estadoVinculoAsignacion("LEGADO_SIN_COINCIDENCIA").build());
        var invalid=sesiones.save(Sesion.builder().alumno(alumno).diaRutina(dias.findById(day).orElseThrow()).fecha(today.minusDays(5)).completada(true).estadoVinculoAsignacion("LEGADO_SIN_COINCIDENCIA").build());
        var de=prescripciones.findById(pressPrescription).orElseThrow();
        series.save(Serie.builder().sesion(invalid).diaEjercicio(de).numeroSerie(0).repsHechas(0).pesoUsado(-3.0).legadoExento(true).incidenciaLegado("VALORES_INVALIDOS").completada(true).build());
        series.save(Serie.builder().sesion(invalid).diaEjercicio(de).numeroSerie(1).repsHechas(8).pesoUsado(90.0).anulada(true).completada(true).build());
        series.save(Serie.builder().sesion(invalid).diaEjercicio(de).numeroSerie(2).repsHechas(4).pesoUsado(70.0).completada(false).build());
        long ancient=start(student,today.minusDays(50));serie(ancient,student,pressPrescription,1,1,999.0);close(ancient,student);
        // Registro ajeno del mismo ejercicio: ni la plantilla compartida ni un ID deben conceder acceso.
        var ar=asignaciones.save(AsignacionRutina.builder().alumno(ajeno).rutina(rutinas.findById(rutina).orElseThrow()).activa(true).fechaInicio(LocalDateTime.now().minusDays(10)).build());
        long third=start(other,today.minusDays(2));serie(third,other,pressPrescription,1,7,300.0);close(third,other);
    }
    @Test void realSummaryExcludesOpenInvalidAnnulledAndOutOfWindowData() throws Exception {
        var r=call(get("/api/alumnos/me/estadisticas"),student,null,200);var m=r.get("datos");
        assertThat(m.get("sesionesRegistradas").asLong()).isEqualTo(5);assertThat(m.get("sesionesAbiertas").asLong()).isEqualTo(1);assertThat(m.get("sesionesFinalizadas").asLong()).isEqualTo(4);
        assertThat(m.get("entrenamientosConDatosValidos").asLong()).isEqualTo(2);assertThat(m.get("sesionesFinalizadasSinDatosValidos").asLong()).isEqualTo(2);
        assertThat(m.get("seriesValidas").asLong()).isEqualTo(5);assertThat(m.get("seriesExcluidas").asLong()).isEqualTo(3);assertThat(m.get("seriesEnSesionesAbiertas").asLong()).isEqualTo(1);
        assertThat(m.get("repeticionesTotales").asLong()).isEqualTo(38);assertThat(m.get("seriesSinPeso").asLong()).isEqualTo(1);assertThat(m.get("seriesConPeso").asLong()).isEqualTo(4);
        assertThat(m.get("pesoMaximo").isTextual()).isTrue();assertThat(new java.math.BigDecimal(m.get("pesoMaximo").asText())).isEqualByComparingTo("60");
        assertThat(new java.math.BigDecimal(m.get("volumenRegistrado").asText())).isEqualByComparingTo("1100");assertThat(m.get("volumenCompleto").asBoolean()).isFalse();
        assertThat(m.get("diasConEntrenamiento").asLong()).isEqualTo(2);assertThat(m.get("alumnosConEntrenamiento").asLong()).isEqualTo(1);assertThat(r.at("/frecuencia/entrenamientosPorSemana").asText()).isEqualTo("0.4667");
        assertThat(r.toString()).doesNotContain("password","token","asistencia","duracion","imc");
    }
    @ParameterizedTest @EnumSource(Rol.class)
    void dashboardsReturnOnlyEachRolesScope(Rol role) throws Exception {
        var r=call(get(dashboard(role)),tokenFor(role),null,200);var m=r.get("entrenamientos");
        assertThat(m.get("entrenamientosConDatosValidos").asLong()).isEqualTo(role==Rol.ADMIN?3:2);
        assertThat(m.get("alumnosConEntrenamiento").asLong()).isEqualTo(role==Rol.ADMIN?2:1);
        if(role==Rol.ADMIN){assertThat(r.at("/estadoActual/usuarios").asLong()).isEqualTo(users.count());assertThat(r.at("/estadoActual/rutinas").asLong()).isEqualTo(1);}
        if(role==Rol.PROFESOR){assertThat(r.get("alumnosVinculados").asLong()).isEqualTo(2);assertThat(r.get("rutinasPropias").asLong()).isEqualTo(1);assertThat(r.get("asignacionesActivasDeSusAlumnos").asLong()).isEqualTo(1);}
        if(role==Rol.ALUMNO){assertThat(r.at("/rutinaActiva/id").asLong()).isEqualTo(assignment);assertThat(r.at("/rutinaActiva/numeroVersion").asInt()).isEqualTo(1);}
    }
    @Test void professorStudentPagesIncludeZerosAndNeverDuplicateSharedLinks() throws Exception {
        var first=call(get("/api/profesores/me/estadisticas/alumnos").param("tamanio","1"),teacher,null,200);
        assertThat(first.get("totalElementos").asLong()).isEqualTo(2);assertThat(first.get("contenido").size()).isEqualTo(1);assertThat(first.at("/contenido/0/alumnoId").asLong()).isEqualTo(alumno.getId());
        var second=call(get("/api/profesores/me/estadisticas/alumnos").param("tamanio","1").param("pagina","1"),teacher,null,200);
        assertThat(second.at("/contenido/0/alumnoId").asLong()).isEqualTo(cero.getId());assertThat(second.at("/contenido/0/datos/entrenamientosConDatosValidos").asLong()).isZero();
        assertThat(second.at("/contenido/0/datos/volumenRegistrado").isNull()).isTrue();
        call(put("/api/admin/alumnos/"+alumno.getId()+"/profesores/"+nuevo.getId()),admin,"{\"motivo\":\"Compartir vínculo\"}",200);
        assertThat(call(get("/api/admin/estadisticas"),admin,null,200).at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(3);
    }
    @Test void dateBoundsUseEffectiveDateAndOriginalDateRemainsVisibleInEvolution() throws Exception {
        var original=call(get("/api/alumnos/me/estadisticas").param("desde",today.minusDays(10).toString()).param("hasta",today.minusDays(10).toString()),student,null,200);
        assertThat(original.at("/datos/sesionesRegistradas").asLong()).isZero();
        var effective=call(get("/api/alumnos/me/estadisticas").param("desde",today.minusDays(9).toString()).param("hasta",today.minusDays(9).toString()),student,null,200);
        assertThat(effective.at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(1);assertThat(effective.at("/frecuencia/entrenamientosPorSemana").asText()).isEqualTo("7.0000");
        var evolution=call(get(evolution()).param("tamanio","1"),student,null,200);
        assertThat(evolution.get("totalElementos").asLong()).isEqualTo(2);assertThat(evolution.at("/contenido/0/sesionId").asLong()).isEqualTo(firstSession);
        assertThat(evolution.at("/contenido/0/fechaOriginal").asText()).isEqualTo(today.minusDays(10).toString());assertThat(evolution.at("/contenido/0/fechaEfectiva").asText()).isEqualTo(today.minusDays(9).toString());
        assertThat(evolution.at("/contenido/0/nombreHistorico").asText()).isEqualTo("Press original");
        assertThat(call(get(evolution()).param("tamanio","1").param("pagina","1"),student,null,200).at("/contenido/0/sesionId").asLong()).isEqualTo(secondSession);
    }
    @Test void activityIncludesOpenAndEmptyDaysButOnlyValidClosedDataCountsAsTraining() throws Exception {
        var activity=call(get("/api/alumnos/me/estadisticas/actividad").param("tamanio","2"),student,null,200);
        assertThat(activity.get("totalElementos").asLong()).isEqualTo(5);assertThat(activity.get("totalPaginas").asLong()).isEqualTo(3);
        var nextPage=call(get("/api/alumnos/me/estadisticas/actividad").param("tamanio","2").param("pagina","1"),student,null,200);
        assertThat(nextPage.at("/contenido/0/fecha").asText()).isEqualTo(today.minusDays(7).toString());assertThat(nextPage.at("/contenido/0/datos/entrenamientosConDatosValidos").asLong()).isZero();
        assertThat(nextPage.at("/contenido/0/datos/seriesEnSesionesAbiertas").asLong()).isEqualTo(1);
        assertThat(nextPage.at("/contenido/1/datos/sesionesFinalizadasSinDatosValidos").asLong()).isEqualTo(1);
    }
    @Test void exerciseStatisticsUseOnlyRealLoadsAndPreserveOriginalNamesAfterTemplateArchival() throws Exception {
        var before=call(get("/api/alumnos/me/estadisticas"),student,null,200).get("datos");
        call(put("/api/ejercicios/"+pressId),teacher,"{\"nombre\":\"Press nuevo\",\"descripcion\":\"Nuevo\",\"grupoMuscular\":\"PECHO\"}",200);
        call(put("/api/rutinas/"+rutina),teacher,"{\"nombre\":\"Plan nuevo\",\"descripcion\":\"Actualizado\"}",200);call(delete("/api/rutinas/"+rutina),teacher,null,204);
        assertThat(call(get("/api/alumnos/me/estadisticas"),student,null,200).get("datos")).isEqualTo(before);
        var list=call(get("/api/alumnos/me/estadisticas/ejercicios").param("tamanio","1"),student,null,200);assertThat(list.get("totalElementos").asLong()).isEqualTo(2);
        assertThat(list.at("/contenido/0/nombreActual").asText()).isEqualTo("Press nuevo");assertThat(list.at("/contenido/0/datos/repeticionesTotales").asLong()).isEqualTo(33);
        assertThat(new java.math.BigDecimal(list.at("/contenido/0/datos/volumenRegistrado").asText())).isEqualByComparingTo("1000");
        assertThat(call(get(evolution()),student,null,200).at("/contenido/0/nombreHistorico").asText()).isEqualTo("Press original");
    }
    @Test void teacherReplacementImmediatelyChangesAllStatisticsAccessWithoutLosingHistory() throws Exception {
        String path="/api/profesores/me/alumnos/"+alumno.getId()+"/estadisticas";
        call(get(path),next,null,403);
        call(put("/api/admin/alumnos/"+alumno.getId()+"/profesores/reemplazo"),admin,"{\"profesorAnteriorId\":"+profesor.getId()+",\"profesorNuevoId\":"+nuevo.getId()+",\"motivo\":\"Cambio\"}",200);
        call(get(path),teacher,null,403);var r=call(get(path),next,null,200);assertThat(r.at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(2);
        assertThat(call(get("/api/profesores/me/estadisticas"),teacher,null,200).at("/datos/entrenamientosConDatosValidos").asLong()).isZero();
        assertThat(call(get("/api/profesores/me/estadisticas"),next,null,200).at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(3);
        call(delete("/api/admin/alumnos/"+alumno.getId()+"/profesores/"+nuevo.getId()),admin,"{\"motivo\":\"Fin\"}",200);call(get(path),next,null,403);call(get("/api/alumnos/me/estadisticas"),student,null,200);
    }
    @ParameterizedTest @EnumSource(Rol.class)
    void namespacesAndManualIdsCannotBypassRoleOrOwnership(Rol role) throws Exception {
        String token=tokenFor(role);
        for(Rol target:Rol.values())if(target!=role)call(get(dashboard(target)),token,null,403);
        if(role!=Rol.ADMIN)for(String suffix:List.of("","/actividad","/ejercicios","/ejercicios/"+pressId+"/evolucion"))call(get("/api/admin/estadisticas"+suffix),token,null,403);
        for(String suffix:List.of("","/actividad","/ejercicios","/ejercicios/"+pressId+"/evolucion")) {
            call(get("/api/alumnos/me/estadisticas"+suffix).param("alumnoId",ajeno.getId().toString()),student,null,200);
            call(get("/api/profesores/me/alumnos/"+ajeno.getId()+"/estadisticas"+suffix),teacher,null,403);
        }
        call(get("/api/admin/dashboard"),null,null,401);call(get("/api/alumnos/me/estadisticas"),null,null,401);
    }
    @Test void serviceScopeChecksAreIndependentOfControllerAndRejectUnauthorizedIds() {
        var d=new CustomUserDetails(users.findById(su.getId()).orElseThrow());var auth=new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities());SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertThatThrownBy(()->service.resumen(EstadisticaService.Alcance.GIMNASIO,null,new EstadisticaFiltro(),auth)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.resumen(EstadisticaService.Alcance.ALUMNO,ajeno.getId(),new EstadisticaFiltro(),auth)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->service.alumnosProfesor(new EstadisticaFiltro(),auth)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }finally{SecurityContextHolder.clearContext();}
    }
    @Test void invalidFiltersAndMissingResourcesUseSafeStatusCodes() throws Exception {
        for(String query:List.of("pagina=-1","tamanio=0","tamanio=101","desde=not-a-date","desde="+today+"&hasta="+today.minusDays(1),"desde="+today.minusDays(367)+"&hasta="+today))
            call(get("/api/alumnos/me/estadisticas?"+query),student,null,400);
        call(get("/api/admin/alumnos/99999999/estadisticas"),admin,null,404);call(get("/api/alumnos/me/estadisticas/ejercicios/99999999/evolucion"),student,null,404);
        var empty=ejercicios.save(Ejercicio.builder().nombre("Sin registros").descripcion("Ninguno").build());assertThat(call(get("/api/alumnos/me/estadisticas/ejercicios/"+empty.getId()+"/evolucion"),student,null,200).get("totalElementos").asLong()).isZero();
        assertThat(call(get("/api/alumnos/me/estadisticas/actividad").param("pagina","100"),student,null,200).get("contenido").size()).isZero();
    }
    @Test void controlledSeriesCorrectionsAndAnnulmentRecomputeStatisticsWithoutChangingOriginalDate() throws Exception {
        var detail=call(get("/api/sesiones/"+secondSession),student,null,200);var first=detail.at("/series/0");
        var body=json.createObjectNode().put("numeroSerie",1).put("repsHechas",10).put("pesoUsado",65).put("revision",first.get("revision").asLong()).put("motivo","Corrección de carga");
        call(patch("/api/sesiones/"+secondSession+"/series/"+first.get("id").asLong()),student,body.toString(),200);
        var corrected=call(get("/api/alumnos/me/estadisticas"),student,null,200);assertThat(new java.math.BigDecimal(corrected.at("/datos/volumenRegistrado").asText())).isEqualByComparingTo("1150");
        assertThat(corrected.at("/datos/pesoMaximo").asText()).isEqualTo("65.0");
        var series2=call(get("/api/sesiones/"+secondSession),student,null,200).at("/series/0");
        call(delete("/api/sesiones/"+secondSession+"/series/"+series2.get("id").asLong()),student,"{\"revision\":"+series2.get("revision").asLong()+",\"motivo\":\"Anulada\"}",200);
        var after=call(get("/api/alumnos/me/estadisticas"),student,null,200);assertThat(new java.math.BigDecimal(after.at("/datos/volumenRegistrado").asText())).isEqualByComparingTo("500");
        assertThat(after.at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(2);
    }
    @Test void inactiveOrRevokedAccountsCannotUseStatisticsAndReactivationDoesNotReviveOldTokens() throws Exception {
        call(put("/api/admin/usuarios/"+su.getId()+"/estado"),admin,"{\"activo\":false,\"revision\":0,\"motivo\":\"Baja\"}",200);call(get("/api/alumnos/me/dashboard"),student,null,401);
        call(put("/api/admin/usuarios/"+su.getId()+"/estado"),admin,"{\"activo\":true,\"revision\":1,\"motivo\":\"Reingreso\"}",200);call(get("/api/alumnos/me/dashboard"),student,null,401);
        assertThat(call(get("/api/admin/alumnos/"+alumno.getId()+"/estadisticas"),admin,null,200).at("/datos/entrenamientosConDatosValidos").asLong()).isEqualTo(2);
    }
    @Test void angularCorsAndOpenApiExposeTheActualTypedContract() throws Exception {
        mvc.perform(options("/api/alumnos/me/dashboard").header("Origin","http://localhost:4200").header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","authorization"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(get("/api/alumnos/me/dashboard").header("Authorization","Bearer "+student).header("Origin","http://localhost:4200"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        var spec=call(get("/v3/api-docs"),null,null,200);
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(spec));assertThat(spec.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        for(String path:List.of("/api/admin/dashboard","/api/profesores/me/dashboard","/api/alumnos/me/dashboard","/api/alumnos/me/estadisticas/actividad","/api/alumnos/me/estadisticas/ejercicios/{ejercicioId}/evolucion")) {
            var op=spec.get("paths").get(path).get("get");assertThat(op.get("responses").has("200")).isTrue();assertThat(op.get("responses").has("401")).isTrue();assertThat(op.get("parameters").toString()).contains("desde","hasta").doesNotContain("Authentication");
        }
        assertThat(spec.get("paths").get("/api/auth/login").get("post").get("security").size()).isZero();
        var schema=spec.at("/components/schemas/Metricas/properties/volumenRegistrado");assertThat(schema.get("type").asText()).isEqualTo("string");
        assertThat(spec.at("/components/schemas/LoginRequest/properties/password/writeOnly").asBoolean()).isTrue();
        assertThat(schema.get("nullable").asBoolean()).isTrue();
        for(String path:List.of("/api/auth/register/alumno","/api/auth/register/profesor","/api/rutinas","/api/ejercicios","/api/sesiones","/api/sesiones/{id}/series"))
            assertThat(spec.get("paths").get(path).get("post").get("responses").has("201")).isTrue();
        assertThat(spec.get("paths").get("/api/rutinas/{id}").get("delete").get("responses").has("204")).isTrue();
        assertThat(spec.get("paths").get("/api/admin/usuarios").get("get").get("parameters").toString()).contains("pagina","tamanio","rol").doesNotContain("\"name\":\"f\"");
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
    private String definition(){return "{\"nombre\":\"Plan original\",\"descripcion\":\"Histórico\",\"dias\":[{\"nombre\":\"Día\",\"numeroOrden\":1,\"ejercicios\":[{\"ejercicioId\":"+pressId+",\"seriesSugeridas\":3,\"repsSugeridas\":8,\"pesoSugerido\":50},{\"ejercicioId\":"+squatId+",\"seriesSugeridas\":1,\"repsSugeridas\":10,\"pesoSugerido\":20}]}]}";}
    private Alumno alumno(Usuario u,String nombre,Profesor p){return alumnos.save(Alumno.builder().nombre(nombre).apellido("Rios").edad(25).email(u.getEmail()).usuario(u).nivel(Nivel.INTERMEDIO).objetivo(Objetivo.MEJORAR_RESISTENCIA).profesores(new ArrayList<>(List.of(p))).build());}
    private Usuario user(String n,Rol r){return users.save(Usuario.builder().email(n+"@test.com").rol(r).password(encoder.encode("InitialPassword123!")).build());}
    private String token(Usuario u){return jwt.generateToken(new CustomUserDetails(u));}
    private String tokenFor(Rol r){return r==Rol.ADMIN?admin:r==Rol.PROFESOR?teacher:student;}
    private String dashboard(Rol r){return r==Rol.ADMIN?"/api/admin/dashboard":r==Rol.PROFESOR?"/api/profesores/me/dashboard":"/api/alumnos/me/dashboard";}
    private String evolution(){return "/api/alumnos/me/estadisticas/ejercicios/"+pressId+"/evolucion";}
    private long start(String t,LocalDate fecha)throws Exception{return call(post("/api/sesiones"),t,"{\"diaRutinaId\":"+day+",\"fecha\":\""+fecha+"\"}",201).get("id").asLong();}
    private void serie(long session,String t,long prescription,int num,int reps,Double peso)throws Exception{call(post("/api/sesiones/"+session+"/series"),t,"{\"diaEjercicioId\":"+prescription+",\"numeroSerie\":"+num+",\"repsHechas\":"+reps+",\"pesoUsado\":"+peso+"}",201);}
    private void close(long session,String t)throws Exception{call(put("/api/sesiones/"+session+"/completar"),t,null,200);}
    private JsonNode call(MockHttpServletRequestBuilder r,String t,String body,int expected)throws Exception{
        if(t!=null)r.header("Authorization","Bearer "+t);if(body!=null)r.contentType(MediaType.APPLICATION_JSON).content(body);
        String content=mvc.perform(r).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return content.isEmpty()?json.nullNode():json.readTree(content);
    }
}
