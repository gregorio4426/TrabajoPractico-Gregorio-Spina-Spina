package utn.simulacro_nombreAlumno.training;

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
class TrainingConsistencyTest {
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


    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void cannotFinishEmptyButCanFinishPartialAndNeverAppendAfterClosure() throws Exception {
        JsonNode open = start(dia1); long id = open.get("id").asLong();
        response(put("/api/sesiones/" + id + "/completar"), student, null, 400);
        add(id, 1, 8, "null", 201);
        JsonNode closed = response(put("/api/sesiones/" + id + "/completar"), student, null, 200);
        assertThat(closed.get("completada").asBoolean()).isTrue();
        assertThat(closed.get("finalizada").asBoolean()).isTrue();
        assertThat(closed.get("diaCumplido").asBoolean()).isFalse();
        assertThat(closed.get("seriesRealizadas").asLong()).isEqualTo(1);
        assertThat(closed.get("seriesEsperadas").asLong()).isEqualTo(3);
        assertThat(closed.get("finalizadaEn").isNull()).isFalse();
        add(id, 2, 8, "50", 400);
        response(put("/api/sesiones/" + id + "/completar"), student, null, 400);
    }

    @Test
    void prescriptionCanBeFulfilledWithStableSeriesOrdering() throws Exception {
        long id = start(dia1).get("id").asLong();
        add(id, 3, 8, "0", 201); add(id, 1, 8, "null", 201); add(id, 2, 9, "50", 201);
        JsonNode closed = response(put("/api/sesiones/" + id + "/completar"), student, null, 200);
        assertThat(closed.get("diaCumplido").asBoolean()).isTrue();
        assertThat(closed.get("seriesRealizadas").asLong()).isEqualTo(3);
        assertThat(closed.get("series").findValuesAsText("numeroSerie")).containsExactly("1", "2", "3");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "{\"numeroSerie\":0,\"repsHechas\":8}", "{\"numeroSerie\":1,\"repsHechas\":0}",
        "{\"numeroSerie\":1,\"repsHechas\":-1}", "{\"numeroSerie\":1}",
        "{\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":-1}",
        "{\"numeroSerie\":1.5,\"repsHechas\":8}", "{\"numeroSerie\":1,\"repsHechas\":8.5}",
        "{\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":\"NaN\"}",
        "{\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":\"Infinity\"}"
    })
    void invalidSeriesNeverPersist(String values) throws Exception {
        long id = start(dia1).get("id").asLong(), before = series.count();
        String body = "{\"diaEjercicioId\":" + prescripcion1 + "," + values.substring(1);
        response(post("/api/sesiones/" + id + "/series"), student, body, 400);
        assertThat(series.count()).isEqualTo(before);
    }

    @Test
    void nestedInvalidPlansAreRejectedWithoutChangingVersionHistory() throws Exception {
        long before = rutinas.count();
        response(put(routinePath()), teacher, definition("Mal", 0, ejercicio.getId()), 400);
        response(post("/api/rutinas"), teacher, "{\"nombre\":\"Plan\",\"descripcion\":\"Texto\",\"dias\":[{\"nombre\":\"Día\",\"numeroOrden\":1,\"ejercicios\":[]}]}", 400);
        response(post("/api/rutinas"), teacher, "{\"nombre\":\"Plan\",\"descripcion\":\"Texto\"}", 400);
        assertThat(rutinas.count()).isEqualTo(before);
        assertThat(response(get("/api/rutinas/me/activa"), student, null, 200).get("numeroVersion").asInt()).isEqualTo(1);
    }

    @Test
    void duplicateNumberIsPerCatalogExerciseEvenAcrossDifferentPrescriptions() throws Exception {
        String exercise = "{\"ejercicioId\":" + ejercicio.getId() + ",\"seriesSugeridas\":1,\"repsSugeridas\":8}";
        String plan = "{\"nombre\":\"Repetido\",\"descripcion\":\"Dos bloques\",\"dias\":[{\"nombre\":\"Día\",\"numeroOrden\":1,\"ejercicios\":[" + exercise + "," + exercise + "]}]}";
        JsonNode version = response(put(routinePath()), teacher, plan, 200);
        response(post(assignPath()), teacher, null, 200);
        long id = start(version.at("/dias/0/id").asLong()).get("id").asLong();
        long first = version.at("/dias/0/ejercicios/0/id").asLong(), second = version.at("/dias/0/ejercicios/1/id").asLong();
        response(post("/api/sesiones/" + id + "/series"), student, seriesBody(first, 1, 8, "50"), 201);
        response(post("/api/sesiones/" + id + "/series"), student, seriesBody(second, 1, 8, "50"), 409);
        response(post("/api/sesiones/" + id + "/series"), student, seriesBody(second, 2, 8, "50"), 201);
        assertThat(response(put("/api/sesiones/" + id + "/completar"), student, null, 200).get("diaCumplido").asBoolean()).isTrue();
    }

    @Test
    void correctingClosedSeriesKeepsOriginalEvidenceAndRejectsStaleRevision() throws Exception {
        JsonNode original = response(get("/api/sesiones/" + sesion1), student, null, 200);
        long serie = original.at("/series/0/id").asLong();
        long revision = original.at("/series/0/revision").asLong();
        JsonNode corrected = response(patch(seriesPath(sesion1, serie)), student, correction(revision, 1, 8, 60), 200);
        assertThat(corrected.get("revision").asLong()).isEqualTo(revision + 1);
        assertThat(corrected.get("pesoUsado").asDouble()).isEqualTo(60);
        assertThat(corrected.get("fecha").asText()).isEqualTo(fecha.toString());
        response(patch(seriesPath(sesion1, serie)), student, correction(revision, 1, 9, 70), 409);
        for (String token : List.of(student, teacher, admin)) {
            JsonNode log = response(get("/api/sesiones/" + sesion1 + "/auditoria"), token, null, 200);
            assertThat(log.size()).isEqualTo(1);
            assertThat(log.get(0).get("accion").asText()).isEqualTo("CORREGIR_SERIE");
            assertThat(log.at("/0/antes/repsHechas").asInt()).isEqualTo(7);
            assertThat(log.at("/0/antes/pesoUsado").asDouble()).isEqualTo(52.5);
            assertThat(log.at("/0/despues/pesoUsado").asDouble()).isEqualTo(60);
            assertThat(log.toString()).doesNotContain("password", "Bearer", "token");
        }
        response(get("/api/sesiones/" + sesion1 + "/auditoria"), next, null, 403);
        response(put(routinePath()), teacher, definition("Versión nueva", 12, ejercicio.getId()), 200);
        mvc.perform(as(delete(routinePath()), teacher)).andExpect(status().isNoContent());
        JsonNode after = response(get("/api/sesiones/" + sesion1), student, null, 200);
        assertThat(after.get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        assertThat(after.at("/series/0/pesoUsado").asDouble()).isEqualTo(60);
    }

    @Test
    void annullingIsSoftAndExcludedFromProgressButCannotEmptyClosedTraining() throws Exception {
        long id = start(dia1).get("id").asLong();
        JsonNode first = add(id, 1, 8, "50", 201), second = add(id, 2, 8, "55", 201);
        JsonNode cancelled = response(delete(seriesPath(id, first.get("id").asLong())), student, reason(first.get("revision").asLong()), 200);
        assertThat(cancelled.get("anulada").asBoolean()).isTrue();
        JsonNode replacement = add(id, 1, 8, "52", 201);
        JsonNode session = response(put("/api/sesiones/" + id + "/completar"), student, null, 200);
        assertThat(session.get("series").size()).isEqualTo(3);
        assertThat(session.get("seriesRealizadas").asLong()).isEqualTo(2);
        JsonNode last = response(get("/api/sesiones/me/ejercicios/" + ejercicio.getId() + "/ultima"), student, null, 200);
        assertThat(last.get("series").size()).isEqualTo(2);
        assertThat(last.get("series").get(0).get("id").asLong()).isEqualTo(replacement.get("id").asLong());
        response(delete(seriesPath(id, second.get("id").asLong())), student, reason(second.get("revision").asLong()), 200);
        response(delete(seriesPath(id, replacement.get("id").asLong())), student, reason(replacement.get("revision").asLong()), 400);
        response(patch(seriesPath(id, first.get("id").asLong())), student, correction(cancelled.get("revision").asLong(), 3, 8, 60), 400);
    }

    @Test
    void sessionDateCorrectionsAreAuditedAndNeverOverwriteOriginalDateOrAssignment() throws Exception {
        JsonNode before = response(get("/api/sesiones/" + sesion1), student, null, 200);
        long rev = before.get("revision").asLong();
        String body = "{\"fecha\":\"" + fecha.minusDays(2) + "\",\"motivo\":\"Fecha anotada incorrectamente\",\"revision\":" + rev + "}";
        JsonNode corrected = response(patch("/api/sesiones/" + sesion1 + "/correccion"), student, body, 200);
        assertThat(corrected.get("fecha").asText()).isEqualTo(fecha.toString());
        assertThat(corrected.get("fechaEfectiva").asText()).isEqualTo(fecha.minusDays(2).toString());
        assertThat(corrected.get("asignacionId")).isEqualTo(before.get("asignacionId"));
        assertThat(corrected.get("rutinaVersionId")).isEqualTo(before.get("rutinaVersionId"));
        response(patch("/api/sesiones/" + sesion1 + "/correccion"), student, body, 409);
        JsonNode log = response(get("/api/sesiones/" + sesion1 + "/auditoria"), student, null, 200);
        assertThat(log.at("/0/antes/fechaOriginal").asText()).isEqualTo(fecha.toString());
        assertThat(log.at("/0/despues/fechaEfectiva").asText()).isEqualTo(fecha.minusDays(2).toString());
        JsonNode last = response(get("/api/sesiones/me/ejercicios/" + ejercicio.getId() + "/ultima"), student, null, 200);
        assertThat(last.get("fechaEfectiva").asText()).isEqualTo(fecha.minusDays(2).toString());
    }

    @Test
    void correctionEndpointsPreserveOwnershipAndTeacherRevocation() throws Exception {
        JsonNode session = response(get("/api/sesiones/" + sesion1), student, null, 200);
        long serie = session.at("/series/0/id").asLong();
        for (String token : List.of(teacher, next, admin)) {
            response(patch(seriesPath(sesion1, serie)), token, correction(0, 1, 8, 60), 403);
            response(delete(seriesPath(sesion1, serie)), token, reason(0), 403);
            response(patch("/api/sesiones/" + sesion1 + "/correccion"), token,
                    "{\"fecha\":\"" + fecha + "\",\"motivo\":\"Cambio\",\"revision\":0}", 403);
        }
        var otherUser = user("ajeno-" + UUID.randomUUID(), Rol.ALUMNO);
        alumnos.save(Alumno.builder().nombre("Ajeno").apellido("Alumno").edad(25).email(otherUser.getEmail()).usuario(otherUser).build());
        response(patch(seriesPath(sesion1, serie)), token(otherUser), correction(0, 1, 8, 60), 403);
        response(get("/api/sesiones/" + sesion1 + "/auditoria"), token(otherUser), null, 403);
        long ownOtherSession = start(dia1).get("id").asLong();
        response(patch(seriesPath(ownOtherSession, serie)), student, correction(0, 1, 8, 60), 404);
        response(patch(seriesPath(sesion1, serie)), student, correction(0, 1, 8, 60), 200);
        response(put("/api/alumnos/" + alumno.getId() + "/profesores/" + profesor.getId() + "/reemplazo/" + nuevo.getId()), admin, null, 200);
        response(get("/api/sesiones/" + sesion1 + "/auditoria"), teacher, null, 403);
        assertThat(response(get("/api/sesiones/" + sesion1 + "/auditoria"), next, null, 200).size()).isEqualTo(1);
    }

    @Test
    void concurrentRegistrationCannotDuplicateSeriesAndClosurePreventsLaterWrites() throws Exception {
        long id = start(dia1).get("id").asLong();
        var registrations = java.util.stream.IntStream.range(0, 2).mapToObj(i -> CompletableFuture.supplyAsync(() ->
                statusOf(post("/api/sesiones/" + id + "/series"), student, seriesBody(prescripcion1, 1, 8, "50")))).toList();
        CompletableFuture.allOf(registrations.toArray(CompletableFuture[]::new)).join();
        assertThat(registrations.stream().map(CompletableFuture::join).toList()).containsExactlyInAnyOrder(201, 409);
        var register = CompletableFuture.supplyAsync(() -> statusOf(post("/api/sesiones/" + id + "/series"), student, seriesBody(prescripcion1, 2, 8, "50")));
        var finish = CompletableFuture.supplyAsync(() -> statusOf(put("/api/sesiones/" + id + "/completar"), student, null));
        CompletableFuture.allOf(register, finish).join();
        assertThat(finish.join()).isEqualTo(200);
        assertThat(register.join()).isIn(201, 400);
        add(id, 3, 8, "50", 400);
        JsonNode session = response(get("/api/sesiones/" + id), student, null, 200);
        assertThat(session.get("finalizada").asBoolean()).isTrue();
        assertThat(session.get("series").size()).isEqualTo(register.join() == 201 ? 2 : 1);
    }

    @Test
    void concurrentCorrectionsWithSameRevisionHaveOneWinnerAndOneAuditEntry() throws Exception {
        long serie = response(get("/api/sesiones/" + sesion1), student, null, 200).at("/series/0/id").asLong();
        var edits = java.util.stream.IntStream.range(0, 2).mapToObj(i -> CompletableFuture.supplyAsync(() ->
                statusOf(patch(seriesPath(sesion1, serie)), student, correction(0, 1, 8, 60 + i)))).toList();
        CompletableFuture.allOf(edits.toArray(CompletableFuture[]::new)).join();
        assertThat(edits.stream().map(CompletableFuture::join).toList()).containsExactlyInAnyOrder(200, 409);
        JsonNode log = response(get("/api/sesiones/" + sesion1 + "/auditoria"), admin, null, 200);
        assertThat(log.size()).isEqualTo(1);
        assertThat(log.at("/0/antes/pesoUsado").asDouble()).isEqualTo(52.5);
    }

    @Test
    void concurrentAssignmentsRemainAtomicAndDoNotMoveHistoricalSessions() throws Exception {
        long before = asignaciones.count();
        var results = java.util.stream.IntStream.range(0, 2).mapToObj(i -> CompletableFuture.supplyAsync(() -> statusOf(post(assignPath()), teacher, null))).toList();
        CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)).join();
        assertThat(results.stream().map(CompletableFuture::join).toList()).containsOnly(200);
        assertThat(asignaciones.count()).isEqualTo(before + 2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asignaciones_rutina WHERE alumno_id = ? AND activa = true", Long.class, alumno.getId())).isEqualTo(1);
        JsonNode old = response(get("/api/sesiones/" + sesion1), student, null, 200);
        assertThat(old.get("asignacionId").asLong()).isEqualTo(asignacion1);
        assertThat(old.get("rutinaVersionId").asLong()).isEqualTo(v1Id);
        assertThat(old.at("/series/0/pesoUsado").asDouble()).isEqualTo(52.5);
    }

    @Test
    void failedAuditInsertRollsBackSeriesAndSessionRevisions() throws Exception {
        long serie = response(get("/api/sesiones/" + sesion1), student, null, 200).at("/series/0/id").asLong();
        JsonNode before = response(get("/api/sesiones/" + sesion1), student, null, 200);
        jdbc.execute("ALTER TABLE auditoria_entrenamientos ADD CONSTRAINT test_rechazar_auditoria CHECK (motivo <> 'FALLO_TEST')");
        try {
            String body = correction(0, 1, 8, 60).replace("Carga errónea", "FALLO_TEST");
            response(patch(seriesPath(sesion1, serie)), student, body, 409);
            assertThat(response(get("/api/sesiones/" + sesion1), student, null, 200)).isEqualTo(before);
            assertThat(response(get("/api/sesiones/" + sesion1 + "/auditoria"), student, null, 200).size()).isZero();
        } finally {
            boolean mysql = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c -> c.getMetaData().getDatabaseProductName().equals("MySQL"));
            jdbc.execute("ALTER TABLE auditoria_entrenamientos DROP " + (mysql ? "CHECK " : "CONSTRAINT ") + "test_rechazar_auditoria");
        }
    }


    @Autowired utn.simulacro_nombreAlumno.service.SesionService service;

    @Test
    void servicesValidateInvalidValuesWithoutRelyingOnController() throws Exception {
        long id = start(dia1).get("id").asLong(), before = series.count();
        var details = new CustomUserDetails(usuarios.findById(alumno.getUsuario().getId()).orElseThrow());
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        assertThatThrownBy(() -> service.registrarSerie(id, utn.simulacro_nombreAlumno.model.request.SerieRequest.builder()
                .diaEjercicioId(prescripcion1).numeroSerie(1).repsHechas(0).pesoUsado(Double.NaN).build(), auth))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(series.count()).isEqualTo(before);
    }

    @Test
    void explicitRestorationIsAuditedAndCannotStealAnExistingSeriesNumber() throws Exception {
        long id = start(dia1).get("id").asLong();
        JsonNode first = add(id, 1, 8, "50", 201); add(id, 2, 8, "50", 201);
        long serie = first.get("id").asLong();
        JsonNode cancelled = response(delete(seriesPath(id, serie)), student, reason(first.get("revision").asLong()), 200);
        add(id, 1, 8, "50", 201);
        String path = seriesPath(id, serie) + "/restaurar";
        response(put(path), teacher, correction(cancelled.get("revision").asLong(), 3, 8, 50), 403);
        response(put(path), student, correction(cancelled.get("revision").asLong(), 1, 8, 50), 409);
        JsonNode restored = response(put(path), student, correction(cancelled.get("revision").asLong(), 3, 8, 50), 200);
        assertThat(restored.get("id").asLong()).isEqualTo(serie);
        assertThat(restored.get("anulada").asBoolean()).isFalse();
        assertThat(response(put("/api/sesiones/" + id + "/completar"), student, null, 200).get("diaCumplido").asBoolean()).isTrue();
        JsonNode log = response(get("/api/sesiones/" + id + "/auditoria"), student, null, 200);
        assertThat(log.size()).isEqualTo(2);
        assertThat(log.at("/1/accion").asText()).isEqualTo("RESTAURAR_SERIE");
        assertThat(log.at("/1/antes/anulada").asBoolean()).isTrue();
        assertThat(log.at("/1/despues/anulada").asBoolean()).isFalse();
    }


    @Test
    void correctingLegacyIncompleteSeriesAuditsItsOriginalCompletionState() throws Exception {
        long serie = response(get("/api/sesiones/" + sesion1), student, null, 200).at("/series/0/id").asLong();
        jdbc.update("UPDATE series SET completada = false WHERE id = ?", serie);
        JsonNode corrected = response(patch(seriesPath(sesion1, serie)), student, correction(0, 1, 8, 50), 200);
        assertThat(corrected.get("completada").asBoolean()).isTrue();
        JsonNode log = response(get("/api/sesiones/" + sesion1 + "/auditoria"), student, null, 200);
        assertThat(log.at("/0/antes/completada").asBoolean()).isFalse();
        assertThat(log.at("/0/despues/completada").asBoolean()).isTrue();
    }

    private String seriesPath(long session, long serie) { return "/api/sesiones/" + session + "/series/" + serie; }
    private String seriesBody(long plan, int number, int reps, String weight) { return "{\"diaEjercicioId\":" + plan + ",\"numeroSerie\":" + number + ",\"repsHechas\":" + reps + ",\"pesoUsado\":" + weight + "}"; }
    private JsonNode add(long session, int number, int reps, String weight, int status) throws Exception { return response(post("/api/sesiones/" + session + "/series"), student, seriesBody(prescripcion1, number, reps, weight), status); }
    private String correction(long revision, int number, int reps, double weight) { return "{\"revision\":" + revision + ",\"motivo\":\"Carga errónea\",\"numeroSerie\":" + number + ",\"repsHechas\":" + reps + ",\"pesoUsado\":" + weight + "}"; }
    private String reason(long revision) { return "{\"revision\":" + revision + ",\"motivo\":\"Registro duplicado\"}"; }
    private int statusOf(MockHttpServletRequestBuilder request, String token, String body) {
        try {
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
            return mvc.perform(as(request, token)).andReturn().getResponse().getStatus();
        } catch (Exception ex) { throw new RuntimeException(ex); }
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
