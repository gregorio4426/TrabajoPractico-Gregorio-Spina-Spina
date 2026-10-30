package utn.simulacro_nombreAlumno.history;

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
class VersionadoIntegrationTest {
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

    @Test
    void structuralEditKeepsHistoricalGraphAndAssignmentUnchanged() throws Exception {
        JsonNode before = response(get("/api/sesiones/" + sesion1), student, null, 200);
        long sessionCount = sesiones.count(), seriesCount = series.count();
        JsonNode v2 = response(put(routinePath()), teacher, definition("Plan nuevo", 12, ejercicio.getId()), 200);
        assertThat(v2.get("id").asLong()).isEqualTo(rutinaId);
        assertThat(v2.get("rutinaVersionId").asLong()).isNotEqualTo(v1Id);
        assertThat(v2.get("numeroVersion").asInt()).isEqualTo(2);
        assertThat(v2.at("/dias/0/id").asLong()).isNotEqualTo(dia1);
        assertThat(v2.at("/dias/0/ejercicios/0/id").asLong()).isNotEqualTo(prescripcion1);
        assertThat(response(get("/api/sesiones/" + sesion1), student, null, 200)).isEqualTo(before);
        JsonNode active = response(get("/api/rutinas/me/activa"), student, null, 200);
        assertThat(active.get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        assertThat(active.at("/dias/0/ejercicios/0/repsSugeridas").asInt()).isEqualTo(8);
        assertThat(active.get("asignacionId").asLong()).isEqualTo(asignacion1);
        assertThat(before.get("asignacionId").asLong()).isEqualTo(asignacion1);
        assertThat(before.get("estadoVinculoAsignacion").asText()).isEqualTo("REGISTRADA");
        assertThat(before.at("/series/0/fecha").asText()).isEqualTo(fecha.toString());
        assertThat(sesiones.count()).isEqualTo(sessionCount);
        assertThat(series.count()).isEqualTo(seriesCount);
        mvc.perform(as(get(routinePath() + "/versiones"), teacher)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        response(get(routinePath() + "/versiones/1"), teacher, null, 200);
        response(get(routinePath() + "/versiones/999"), teacher, null, 404);
    }

    @Test
    void reassignmentPinsNewSessionsToNewVersionAndOldSessionsKeepOriginalAssignment() throws Exception {
        JsonNode v2 = response(put(routinePath()), teacher, definition("Plan nuevo", 12, ejercicio.getId()), 200);
        long day2 = v2.at("/dias/0/id").asLong();
        response(post("/api/sesiones"), student, startBody(day2), 403);
        JsonNode assignment2 = response(post(assignPath()), teacher, null, 200);
        JsonNode session2 = start(day2);
        assertThat(session2.get("rutinaVersionId").asLong()).isEqualTo(v2.get("rutinaVersionId").asLong());
        assertThat(session2.get("asignacionId").asLong()).isEqualTo(assignment2.get("id").asLong());
        assertThat(session2.get("asignacionId").asLong()).isNotEqualTo(asignacion1);
        response(post("/api/sesiones"), student, startBody(dia1), 403);
        JsonNode previous = response(get("/api/sesiones/" + sesion1), student, null, 200);
        assertThat(previous.get("asignacionId").asLong()).isEqualTo(asignacion1);
        assertThat(previous.get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        assertThat(previous.at("/series/0/pesoUsado").asDouble()).isEqualTo(52.5);
        JsonNode history = response(get("/api/rutinas/me/historial"), student, null, 200);
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).get("id")).isEqualTo(history.get(1).get("id"));
        assertThat(history.get(0).get("rutinaVersionId")).isNotEqualTo(history.get(1).get("rutinaVersionId"));
    }

    @Test
    void archiveIsIdempotentAndKeepsActiveTrainingAndAllHistoricalData() throws Exception {
        JsonNode open = start(dia1);
        long count = rutinas.count(), assignments = asignaciones.count(), training = sesiones.count(), performed = series.count();
        mvc.perform(as(delete(routinePath()), teacher)).andExpect(status().isNoContent());
        mvc.perform(as(delete(routinePath()), teacher)).andExpect(status().isNoContent());
        JsonNode catalog = response(get("/api/rutinas"), teacher, null, 200);
        assertThat(java.util.stream.StreamSupport.stream(catalog.spliterator(), false)
                .map(r -> r.get("id").asLong()).toList()).doesNotContain(rutinaId);
        assertThat(response(get(routinePath()), teacher, null, 200).get("archivada").asBoolean()).isTrue();
        JsonNode history = response(get("/api/rutinas/me/historial"), student, null, 200);
        assertThat(history.get(0).get("archivada").asBoolean()).isTrue();
        assertThat(response(get("/api/sesiones/" + sesion1), teacher, null, 200).at("/series/0/repsHechas").asInt()).isEqualTo(7);
        response(post(assignPath()), teacher, null, 400);
        response(put(routinePath()), teacher, definition("Otra", 9, ejercicio.getId()), 400);
        response(post("/api/sesiones/" + open.get("id").asLong() + "/series"), student,
                "{\"diaEjercicioId\":" + prescripcion1 + ",\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":50}", 201);
        response(put("/api/sesiones/" + open.get("id").asLong() + "/completar"), student, null, 200);
        assertThat(rutinas.count()).isEqualTo(count);
        assertThat(asignaciones.count()).isEqualTo(assignments);
        assertThat(sesiones.count()).isEqualTo(training);
        assertThat(series.count()).isEqualTo(performed + 1);
        assertThat(start(dia1).get("asignacionId").asLong()).isEqualTo(asignacion1);
    }

    @Test
    void metadataOnlyUpdatesAlsoPreserveNamesAndIdenticalPutIsIdempotent() throws Exception {
        JsonNode v2 = response(put(routinePath()), teacher, "{\"nombre\":\"Nombre nuevo\",\"descripcion\":\"Texto\"}", 200);
        assertThat(v2.get("numeroVersion").asInt()).isEqualTo(2);
        assertThat(v2.at("/dias/0/ejercicios/0/repsSugeridas").asInt()).isEqualTo(8);
        JsonNode repeated = response(put(routinePath()), teacher, "{\"nombre\":\"Nombre nuevo\",\"descripcion\":\"Texto\"}", 200);
        assertThat(repeated.get("rutinaVersionId")).isEqualTo(v2.get("rutinaVersionId"));
        assertThat(response(get("/api/sesiones/" + sesion1), student, null, 200).get("rutinaNombre").asText()).isEqualTo("Plan original");
    }

    @Test
    void changingExerciseCatalogCannotRewriteHistoricalExerciseOrDeleteItsIdentity() throws Exception {
        response(put("/api/ejercicios/" + ejercicio.getId()), teacher,
                "{\"nombre\":\"Nombre cambiado\",\"descripcion\":\"Otra descripción\",\"grupoMuscular\":\"ESPALDA\"}", 200);
        JsonNode previous = response(get("/api/sesiones/" + sesion1), student, null, 200);
        assertThat(previous.at("/series/0/ejercicioNombre").asText()).isEqualTo("Press original");
        assertThat(previous.at("/series/0/ejercicioId").asLong()).isEqualTo(ejercicio.getId());
        JsonNode active = response(get("/api/rutinas/me/activa"), student, null, 200);
        assertThat(active.at("/dias/0/ejercicios/0/grupoMuscular").asText()).isEqualTo("PECHO");
        response(delete("/api/ejercicios/" + ejercicio.getId()), teacher, null, 400);
        JsonNode last = response(get("/api/sesiones/me/ejercicios/" + ejercicio.getId() + "/ultima"), student, null, 200);
        assertThat(last.get("ejercicioNombre").asText()).isEqualTo("Press original");
        assertThat(last.at("/series/0/pesoUsado").asDouble()).isEqualTo(52.5);
    }

    @Test
    void nullHistoricalMuscleGroupStaysNullAfterCatalogUpdateAndMetadataCopy() throws Exception {
        var unknown = ejercicios.save(Ejercicio.builder().nombre("Sin grupo").descripcion("Descripción").build());
        response(put(routinePath()), teacher, definition("Grupo desconocido", 8, unknown.getId()), 200);
        response(put("/api/ejercicios/" + unknown.getId()), teacher,
                "{\"nombre\":\"Con grupo\",\"descripcion\":\"Otra\",\"grupoMuscular\":\"PECHO\"}", 200);
        JsonNode copy = response(put(routinePath()), teacher, "{\"nombre\":\"Solo nombre\",\"descripcion\":\"Otro texto\"}", 200);
        assertThat(copy.at("/dias/0/ejercicios/0/ejercicioNombre").asText()).isEqualTo("Sin grupo");
        assertThat(copy.at("/dias/0/ejercicios/0/grupoMuscular").isNull()).isTrue();
        JsonNode oldVersion = response(get(routinePath() + "/versiones/2"), student, null, 200);
        assertThat(oldVersion.at("/dias/0/ejercicios/0/grupoMuscular").isNull()).isTrue();
    }

    @Test
    void failedVersionCreationRollsBackWithoutAlteringHistory() throws Exception {
        long versions = rutinas.count();
        response(put(routinePath()), teacher, definition("Inválida", 12, 99999999L), 404);
        assertThat(rutinas.count()).isEqualTo(versions);
        JsonNode old = response(get("/api/rutinas/me/activa"), student, null, 200);
        assertThat(old.get("numeroVersion").asInt()).isEqualTo(1);
        assertThat(old.get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        assertThat(response(get("/api/sesiones/" + sesion1), student, null, 200).at("/series/0/pesoUsado").asDouble()).isEqualTo(52.5);
    }

    @Test
    void switchingTeacherAfterEditAndArchivePreservesCompleteHistoryAndPermissions() throws Exception {
        response(put(routinePath()), teacher, definition("Plan nuevo", 12, ejercicio.getId()), 200);
        mvc.perform(as(delete(routinePath()), teacher)).andExpect(status().isNoContent());
        String path = "/api/alumnos/" + alumno.getId() + "/profesores/" + profesor.getId() + "/reemplazo/" + nuevo.getId();
        response(put(path), admin, null, 200);
        response(get("/api/sesiones/" + sesion1), teacher, null, 403);
        assertThat(response(get("/api/sesiones/" + sesion1), next, null, 200).at("/series/0/pesoUsado").asDouble()).isEqualTo(52.5);
        assertThat(response(get("/api/rutinas/historial/alumno/" + alumno.getId()), next, null, 200).get(0).get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        response(put(routinePath()), next, definition("No autorizada", 9, ejercicio.getId()), 403);
        response(delete(routinePath()), student, null, 403);
        response(delete(routinePath()), admin, null, 403);
        response(get("/api/sesiones/" + sesion1), admin, null, 200);
    }

    @Test
    void concurrentEditsCreateDistinctSequentialVersionsWithoutLosingOriginal() throws Exception {
        var requests = java.util.stream.IntStream.range(0, 2).mapToObj(i -> CompletableFuture.supplyAsync(() -> {
            try { return response(put(routinePath()), teacher, definition("Concurrente " + i, 10 + i, ejercicio.getId()), 200); }
            catch (Exception e) { throw new RuntimeException(e); }
        })).toList();
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();
        assertThat(requests.stream().map(f -> f.join().get("numeroVersion").asInt()).toList()).containsExactlyInAnyOrder(2, 3);
        JsonNode versions = response(get(routinePath() + "/versiones"), teacher, null, 200);
        assertThat(versions.size()).isEqualTo(3);
        assertThat(versions.get(0).get("nombre").asText()).isEqualTo("Plan original");
        assertThat(response(get("/api/sesiones/" + sesion1), student, null, 200).get("rutinaVersionId").asLong()).isEqualTo(v1Id);
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
        var result = mvc.perform(as(request, token)).andExpect(status().is(status)).andReturn();
        String content = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return content.isEmpty() ? json.nullNode() : json.readTree(content);
    }
}
