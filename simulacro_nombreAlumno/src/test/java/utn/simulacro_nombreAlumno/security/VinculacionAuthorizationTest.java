package utn.simulacro_nombreAlumno.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Usa login, JWT, filtros, method security, services, mappers y JPA reales con H2. */
@SpringBootTest
@AutoConfigureMockMvc
class VinculacionAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProfesorRepository profesores;
    @Autowired AlumnoRepository alumnos;
    @Autowired RutinaRepository rutinas;
    @Autowired AsignacionRutinaRepository asignaciones;
    @Autowired SesionRepository sesiones;
    @Autowired SerieRepository series;
    @Autowired AlumnoService alumnoService;
    @Autowired ProfesorService profesorService;
    @Autowired RutinaService rutinaService;
    @Autowired SesionService sesionService;
    @Autowired CustomUserDetailsService details;
    private Profesor anterior, nuevo;
    private Alumno alumno, ajeno;
    private Rutina rutinaAnterior, rutinaNueva;
    private Sesion entreno, otroEntreno;
    private String adminEmail, oldEmail, newEmail, studentEmail, otherEmail;
    private String admin, old, next, student, other;

    @BeforeEach
    void fixture() throws Exception {
        String suffix = UUID.randomUUID().toString();
        adminEmail = "admin-" + suffix + "@test.com";
        oldEmail = "old-" + suffix + "@test.com";
        newEmail = "new-" + suffix + "@test.com";
        studentEmail = "student-" + suffix + "@test.com";
        otherEmail = "other-" + suffix + "@test.com";
        user(adminEmail, Rol.ADMIN);
        anterior = profesores.save(Profesor.builder().nombre("Anterior").apellido("Profesor").especialidad("Fuerza")
                .email(oldEmail).usuario(user(oldEmail, Rol.PROFESOR)).build());
        nuevo = profesores.save(Profesor.builder().nombre("Nuevo").apellido("Profesor").especialidad("Resistencia")
                .email(newEmail).usuario(user(newEmail, Rol.PROFESOR)).build());
        alumno = alumnos.save(Alumno.builder().nombre("Propio").apellido("Alumno").edad(25).email(studentEmail)
                .fechaNacimiento(LocalDate.of(2001, 1, 1)).nivel(Nivel.INTERMEDIO).objetivo(Objetivo.MEJORAR_RESISTENCIA)
                .usuario(user(studentEmail, Rol.ALUMNO)).profesores(new ArrayList<>(List.of(anterior))).build());
        ajeno = alumnos.save(Alumno.builder().nombre("Ajeno").apellido("Alumno").edad(25).email(otherEmail)
                .nivel(Nivel.INTERMEDIO).objetivo(Objetivo.MEJORAR_RESISTENCIA)
                .usuario(user(otherEmail, Rol.ALUMNO)).build());
        rutinaAnterior = routine(anterior, "Anterior");
        rutinaNueva = routine(nuevo, "Nueva");
        asignaciones.save(AsignacionRutina.builder().rutina(rutinaAnterior).alumno(alumno).activa(true)
                .fechaInicio(LocalDateTime.of(2026, 9, 1, 12, 0)).build());
        asignaciones.save(AsignacionRutina.builder().rutina(rutinaNueva).alumno(alumno).activa(false)
                .fechaInicio(LocalDateTime.of(2026, 8, 1, 12, 0)).fechaFin(LocalDateTime.of(2026, 9, 1, 12, 0)).build());
        asignaciones.save(AsignacionRutina.builder().rutina(rutinaAnterior).alumno(ajeno).activa(true)
                .fechaInicio(LocalDateTime.of(2026, 9, 1, 12, 0)).build());
        entreno = sesiones.save(Sesion.builder().alumno(alumno).diaRutina(rutinaAnterior.getDias().getFirst())
                .fecha(LocalDate.of(2026, 9, 1)).completada(true).build());
        otroEntreno = sesiones.save(Sesion.builder().alumno(alumno).diaRutina(rutinaNueva.getDias().getFirst())
                .fecha(LocalDate.of(2026, 9, 2)).completada(true).build());
        series.save(Serie.builder().sesion(entreno).diaEjercicio(rutinaAnterior.getDias().getFirst().getEjercicios().getFirst())
                .numeroSerie(1).repsHechas(8).pesoUsado(50.0).completada(true).build());
        admin = login(adminEmail);
        old = login(oldEmail);
        next = login(newEmail);
        student = login(studentEmail);
        other = login(otherEmail);
    }

    @Test
    void myStudentsIsTeacherOnlyAndLegacyListIsScoped() throws Exception {
        mvc.perform(as(get("/api/profesores/me/alumnos"), old)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(alumno.getId()));
        mvc.perform(as(get("/api/profesores/me/alumnos"), next)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        for (String token : List.of(student, admin)) {
            mvc.perform(as(get("/api/profesores/me/alumnos"), token)).andExpect(status().isForbidden());
        }
        mvc.perform(as(get("/api/alumnos"), old)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(alumno.getId()));
        mvc.perform(as(get("/api/alumnos"), next)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(get("/api/alumnos"), admin)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + ajeno.getId() + ")]").isNotEmpty());
        mvc.perform(as(get("/api/alumnos"), student)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void rolesCanReadOnlyAuthorizedStudentData(Rol role) throws Exception {
        String token = token(role);
        mvc.perform(as(get("/api/alumnos/" + alumno.getId()), token)).andExpect(status().isOk());
        mvc.perform(as(get("/api/rutinas/historial/alumno/" + alumno.getId()), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(as(get("/api/rutinas/activa/alumno/" + alumno.getId()), token)).andExpect(status().isOk());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].pesoUsado").value(50.0));
        if (role == Rol.ALUMNO) {
            mvc.perform(as(get("/api/sesiones/me/historial"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        } else {
            mvc.perform(as(get("/api/sesiones/alumno/" + alumno.getId() + "/historial"), token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        }
        var result = mvc.perform(as(get("/api/alumnos/" + ajeno.getId()), token));
        if (role == Rol.ADMIN) result.andExpect(status().isOk());
        else result.andExpect(status().isForbidden());
    }

    @Test
    void changingTeacherPreservesHistoryAndRevokesSameOldToken() throws Exception {
        long sessionsBefore = sesiones.count(), seriesBefore = series.count(), assignmentsBefore = asignaciones.count();
        mvc.perform(as(put(changePath()), admin)).andExpect(status().isOk());
        for (String path : protectedPaths()) {
            mvc.perform(as(get(path), old)).andExpect(status().isForbidden());
            mvc.perform(as(get(path), next)).andExpect(status().isOk());
        }
        mvc.perform(as(get("/api/profesores/me/alumnos"), old)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(get("/api/profesores/me/alumnos"), next)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(alumno.getId()));
        mvc.perform(as(get("/api/sesiones/alumno/" + alumno.getId() + "/historial"), next)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[1].series[0].repsHechas").value(8))
                .andExpect(jsonPath("$[1].series[0].pesoUsado").value(50.0));
        mvc.perform(as(get("/api/rutinas/me/historial"), student)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(as(get("/api/sesiones/me/historial"), student)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(as(post(assignPath(rutinaAnterior)), old)).andExpect(status().isForbidden());
        mvc.perform(as(patch(followPath()).contentType(MediaType.APPLICATION_JSON).content(followBody()), old)).andExpect(status().isForbidden());
        assertThat(sesiones.count()).isEqualTo(sessionsBefore);
        assertThat(series.count()).isEqualTo(seriesBefore);
        assertThat(asignaciones.count()).isEqualTo(assignmentsBefore);
        // El nuevo profesor puede asignar sus plantillas, pero no editar las del anterior.
        mvc.perform(as(post(assignPath(rutinaNueva)), next)).andExpect(status().isOk());
        mvc.perform(as(put("/api/rutinas/" + rutinaAnterior.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cambio\",\"descripcion\":\"Plan\",\"dias\":[]}"), next)).andExpect(status().isForbidden());
    }

    @Test
    void studentCanReplaceOwnTeacherButNobodyCanReplaceSomeoneElsesByUrlId() throws Exception {
        mvc.perform(as(put(changePath()), other)).andExpect(status().isForbidden());
        mvc.perform(as(put(changePath()), next)).andExpect(status().isForbidden());
        mvc.perform(as(put(changePath()), student)).andExpect(status().isOk());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), old)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), next)).andExpect(status().isOk());
    }

    @Test
    void addingAndRemovingLinksIsIdempotentAndDoesNotDeleteHistory() throws Exception {
        String path = "/api/alumnos/" + alumno.getId() + "/profesores/" + nuevo.getId();
        long count = sesiones.count(), assignments = asignaciones.count();
        for (int i = 0; i < 2; i++) mvc.perform(as(post(path), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/profesores/me/alumnos"), next)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        for (int i = 0; i < 2; i++) mvc.perform(as(delete(path), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), next)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), old)).andExpect(status().isOk());
        assertThat(sesiones.count()).isEqualTo(count);
        assertThat(asignaciones.count()).isEqualTo(assignments);
    }

    @Test
    void studentAndTeacherCanEndTheirOwnLinkOnly() throws Exception {
        mvc.perform(as(delete("/api/alumnos/" + alumno.getId() + "/profesores/" + anterior.getId()), other)).andExpect(status().isForbidden());
        mvc.perform(as(delete("/api/profesores/me/alumnos/" + alumno.getId()), next)).andExpect(status().isForbidden());
        mvc.perform(as(delete("/api/alumnos/me/profesores/" + anterior.getId()), student)).andExpect(status().isOk());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), old)).andExpect(status().isForbidden());
        mvc.perform(as(post("/api/alumnos/me/profesores/" + anterior.getId()), student)).andExpect(status().isOk());
        mvc.perform(as(delete("/api/profesores/me/alumnos/" + alumno.getId()), old)).andExpect(status().isNoContent());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), old)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/sesiones/" + entreno.getId()), student)).andExpect(status().isOk());
    }

    @Test
    void relationshipsCannotBeAcquiredByTeacherOrByAnotherStudent() throws Exception {
        String path = "/api/alumnos/" + alumno.getId() + "/profesores/" + nuevo.getId();
        for (String token : List.of(next, other)) {
            mvc.perform(as(post(path), token)).andExpect(status().isForbidden());
            mvc.perform(as(delete(path), token)).andExpect(status().isForbidden());
        }
        mvc.perform(as(post(path), student)).andExpect(status().isOk());
    }

    @Test
    void missingReplacementRollsBackAndEmptyHistoryIsNotForbidden() throws Exception {
        mvc.perform(as(put("/api/alumnos/" + alumno.getId() + "/profesores/" + anterior.getId() + "/reemplazo/99999999"), admin))
                .andExpect(status().isNotFound());
        mvc.perform(as(get("/api/alumnos/" + alumno.getId()), old)).andExpect(status().isOk());
        mvc.perform(as(post("/api/alumnos/" + ajeno.getId() + "/profesores/" + nuevo.getId()), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/sesiones/alumno/" + ajeno.getId() + "/historial"), next)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void followUpIsManagementOnlyWithCurrentLink(Rol role) throws Exception {
        var request = as(patch(followPath()).contentType(MediaType.APPLICATION_JSON).content(followBody()), token(role));
        if (role == Rol.ALUMNO) mvc.perform(request).andExpect(status().isForbidden());
        else mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.nivel").value("AVANZADO"));
        mvc.perform(as(patch(followPath()).contentType(MediaType.APPLICATION_JSON).content(followBody()), next)).andExpect(status().isForbidden());
        mvc.perform(as(patch(followPath()).contentType(MediaType.APPLICATION_JSON).content("{}"), admin)).andExpect(status().isBadRequest());
    }

    @Test
    void profileAndTrainingWritesCannotTargetAnotherPersonsIds() throws Exception {
        String body = "{\"nombre\":\"Nuevo\",\"apellido\":\"Apellido\",\"nivel\":\"INTERMEDIO\",\"objetivo\":\"MEJORAR_RESISTENCIA\"}";
        mvc.perform(as(put("/api/alumnos/" + ajeno.getId()).contentType(MediaType.APPLICATION_JSON).content(body), student)).andExpect(status().isForbidden());
        mvc.perform(as(put("/api/alumnos/" + alumno.getId()).contentType(MediaType.APPLICATION_JSON).content(body), old)).andExpect(status().isForbidden());
        mvc.perform(as(put("/api/alumnos/" + alumno.getId()).contentType(MediaType.APPLICATION_JSON).content(body), admin)).andExpect(status().isOk());
        String teacherBody = "{\"nombre\":\"Nuevo\",\"apellido\":\"Apellido\",\"especialidad\":\"Fuerza\"}";
        mvc.perform(as(patch("/api/profesores/" + nuevo.getId()).contentType(MediaType.APPLICATION_JSON).content(teacherBody), old)).andExpect(status().isForbidden());
        mvc.perform(as(patch("/api/profesores/" + anterior.getId()).contentType(MediaType.APPLICATION_JSON).content(teacherBody), old)).andExpect(status().isOk());
        mvc.perform(as(patch("/api/profesores/" + nuevo.getId()).contentType(MediaType.APPLICATION_JSON).content(teacherBody), admin)).andExpect(status().isOk());
        for (String token : List.of(other, old, admin)) {
            mvc.perform(as(put("/api/sesiones/" + entreno.getId() + "/completar"), token)).andExpect(status().isForbidden());
            mvc.perform(as(post("/api/sesiones/" + entreno.getId() + "/series").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"diaEjercicioId\":1,\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":50}"), token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void catalogsDoNotExposeOtherStudentsNames() throws Exception {
        String path = "/api/rutinas/" + rutinaAnterior.getId();
        for (String token : List.of(student, old)) {
            mvc.perform(as(get(path), token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.alumnos", hasSize(1))).andExpect(jsonPath("$.alumnos[0]").value("Propio Alumno"));
        }
        mvc.perform(as(get(path), next)).andExpect(status().isOk()).andExpect(jsonPath("$.alumnos", hasSize(0)));
        mvc.perform(as(get(path), admin)).andExpect(status().isOk()).andExpect(jsonPath("$.alumnos", hasSize(2)));
    }

    @Test
    void directServiceCallsAlsoEnforcePermissionsWithoutController() {
        var auth = authentication(newEmail);
        assertThatThrownBy(() -> sesionService.getDetalle(entreno.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> sesionService.getHistorialDeAlumno(alumno.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> rutinaService.getHistorialDeAlumno(alumno.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> rutinaService.getRutinaActivaDeAlumno(alumno.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> rutinaService.asignarRutinaMe(alumno.getId(), rutinaNueva.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> alumnoService.actualizarSeguimiento(alumno.getId(), SeguimientoAlumnoRequest.builder()
                .nivel(Nivel.AVANZADO).objetivo(Objetivo.MEJORAR_RESISTENCIA).build(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> alumnoService.elegirProfesor(alumno.getId(), nuevo.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> alumnoService.desvincularProfesor(alumno.getId(), anterior.getId(), auth)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> alumnoService.getMiPerfil(auth)).isInstanceOf(AccessDeniedException.class);
        try {
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThatThrownBy(() -> alumnoService.findAlumnoResponseById(alumno.getId())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> alumnoService.patchNivel(alumno.getId(), Nivel.AVANZADO)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> profesorService.updateProfesor(anterior.getId(), ProfesorRequest.builder().build())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> rutinaService.asignarRutina(rutinaNueva.getId(), alumno.getId())).isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        for (String path : List.of("/api/profesores/me/alumnos", "/api/alumnos", "/api/alumnos/" + alumno.getId(),
                "/api/rutinas/historial/alumno/" + alumno.getId(), "/api/sesiones/" + entreno.getId())) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }


    @Autowired AuthService authService;
    @Autowired EjercicioService ejercicioService;

    @ParameterizedTest
    @EnumSource(Rol.class)
    void teacherRegistrationRemainsAdminOnlyAtHttpAndServiceLayers(Rol role) throws Exception {
        var request = RegisterProfesorRequest.builder().nombre("Alta").apellido("Profesor").especialidad("Fuerza")
                .email("alta-" + UUID.randomUUID() + "@test.com").password("TestPassword123!").build();
        var response = mvc.perform(as(post("/api/auth/register/profesor").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)), token(role)));
        if (role == Rol.ADMIN) response.andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("PROFESOR"));
        else response.andExpect(status().isForbidden());
        request.setEmail("direct-" + UUID.randomUUID() + "@test.com");
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication(role == Rol.ADMIN ? adminEmail : role == Rol.PROFESOR ? oldEmail : studentEmail));
            if (role == Rol.ADMIN) assertThat(authService.registerProfesor(request).getRol()).isEqualTo("PROFESOR");
            else assertThatThrownBy(() -> authService.registerProfesor(request)).isInstanceOf(AccessDeniedException.class);
        } finally { SecurityContextHolder.clearContext(); }
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void exerciseWritesKeepTheirExistingTeacherPermissionInServices(Rol role) {
        var request = EjercicioRequest.builder().nombre("Nuevo").descripcion("Prueba").grupoMuscular(GrupoMuscular.PECHO).build();
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication(role == Rol.ADMIN ? adminEmail : role == Rol.PROFESOR ? oldEmail : studentEmail));
            if (role == Rol.PROFESOR) {
                var created = ejercicioService.createEjercicio(request);
                assertThat(ejercicioService.updateEjercicio(created.getId(), request).getNombre()).isEqualTo("Nuevo");
                ejercicioService.deleteEjercicio(created.getId());
            } else {
                assertThatThrownBy(() -> ejercicioService.createEjercicio(request)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> ejercicioService.updateEjercicio(1L, request)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> ejercicioService.deleteEjercicio(1L)).isInstanceOf(AccessDeniedException.class);
            }
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void publicStudentRegistrationCanChooseTeacherWithInitiallyEmptyRelations() throws Exception {
        String email = "registered-" + UUID.randomUUID() + "@test.com";
        String body = json.writeValueAsString(RegisterAlumnoRequest.builder().nombre("Alta").apellido("Alumno")
                .email(email).password("TestPassword123!").edad(25).fechaNacimiento(LocalDate.of(2001, 1, 1))
                .nivel(Nivel.PRINCIPIANTE).objetivo(Objetivo.MEJORAR_RESISTENCIA).build());
        var response = mvc.perform(post("/api/auth/register/alumno").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String token = json.readTree(response.getContentAsString()).get("token").asText();
        mvc.perform(as(post("/api/alumnos/me/profesores/" + nuevo.getId()), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.profesores", hasSize(1))).andExpect(jsonPath("$.profesores[0].id").value(nuevo.getId()));
    }

    @Test
    void concurrentDuplicateLinksAreSerializedWithoutDuplicateRelationships() {
        var requests = java.util.stream.IntStream.range(0, 3).mapToObj(i -> java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                mvc.perform(as(post("/api/alumnos/me/profesores/" + nuevo.getId()), student)).andExpect(status().isOk());
            } catch (Exception e) { throw new RuntimeException(e); }
        })).toList();
        java.util.concurrent.CompletableFuture.allOf(requests.toArray(java.util.concurrent.CompletableFuture[]::new)).join();
        assertThatCode(() -> mvc.perform(as(get("/api/alumnos/me"), student)).andExpect(status().isOk())
                .andExpect(jsonPath("$.profesores", hasSize(2)))).doesNotThrowAnyException();
    }

    private Usuario user(String email, Rol role) {
        return usuarios.save(Usuario.builder().email(email).password(passwords.encode("TestPassword123!" )).rol(role).build());
    }

    private Rutina routine(Profesor owner, String name) {
        var routine = Rutina.builder().nombre(name).descripcion("Plan").profesor(owner).build();
        var day = DiaRutina.builder().nombre("Día 1").numeroOrden(1).rutina(routine).build();
        // El catálogo se persiste antes de asociarlo a la prescripción.
        var exercise = ejercicioRepository.save(Ejercicio.builder().nombre("Press").descripcion("Banca").grupoMuscular(GrupoMuscular.PECHO).build());
        day.getEjercicios().add(DiaEjercicio.builder().diaRutina(day).ejercicio(exercise).seriesSugeridas(3).repsSugeridas(8).pesoSugerido(40.0).build());
        routine.getDias().add(day);
        return rutinas.save(routine);
    }

    @Autowired EjercicioRepository ejercicioRepository;

    private String login(String email) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(LoginRequest.builder().email(email).password("TestPassword123!").build())))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("token").asText();
    }

    private Authentication authentication(String email) {
        var principal = details.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
    private String token(Rol rol) { return rol == Rol.ADMIN ? admin : rol == Rol.PROFESOR ? old : student; }
    private String changePath() { return "/api/alumnos/" + alumno.getId() + "/profesores/" + anterior.getId() + "/reemplazo/" + nuevo.getId(); }
    private String followPath() { return "/api/alumnos/" + alumno.getId() + "/seguimiento"; }
    private String followBody() { return "{\"nivel\":\"AVANZADO\",\"objetivo\":\"GANAR_MASA_MUSCULAR\"}"; }
    private String assignPath(Rutina rutina) { return "/api/rutinas/asignar/alumno/" + alumno.getId() + "/rutina/" + rutina.getId(); }
    private List<String> protectedPaths() {
        return List.of("/api/alumnos/" + alumno.getId(), "/api/rutinas/historial/alumno/" + alumno.getId(),
                "/api/rutinas/activa/alumno/" + alumno.getId(), "/api/sesiones/" + entreno.getId(),
                "/api/sesiones/" + otroEntreno.getId(), "/api/sesiones/alumno/" + alumno.getId() + "/historial");
    }
}
