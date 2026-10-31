package utn.simulacro_nombreAlumno.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import utn.simulacro_nombreAlumno.controller.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.response.*;
import utn.simulacro_nombreAlumno.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contratos HTTP aislados. La autorización integrada se prueba en la etapa 2. */
class HttpCompatibilityTest {
    private final AlumnoService alumnos = mock(AlumnoService.class);
    private final ProfesorService profesores = mock(ProfesorService.class);
    private final EjercicioService ejercicios = mock(EjercicioService.class);
    private final RutinaService rutinas = mock(RutinaService.class);
    private final SesionService sesiones = mock(SesionService.class);
    private final AuthService auth = mock(AuthService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AlumnoController(alumnos),
                new ProfesorController(profesores, alumnos), new EjercicioController(ejercicios),
                new RutinaController(rutinas), new SesionController(sesiones), new AuthController(auth, mock(utn.simulacro_nombreAlumno.service.IdentidadService.class))).build();
    }

    @Test
    void studentAndTeacherListsRemainArraysWithExistingFieldsAndAddedIds() throws Exception {
        when(alumnos.listarAlumnos()).thenReturn(List.of(AlumnoResponse.builder().id(8L)
                .nombre("Luis").apellido("Rios").email("luis@test.com").edad(25)
                .fechaNacimiento(LocalDate.of(2001, 1, 1)).nivel(Nivel.INTERMEDIO)
                .objetivo(Objetivo.GANAR_MASA_MUSCULAR).profesores(List.of()).build()));
        mvc.perform(get("/api/alumnos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8)).andExpect(jsonPath("$[0].nombre").value("Luis"))
                .andExpect(jsonPath("$[0].apellido").value("Rios")).andExpect(jsonPath("$[0].edad").value(25))
                .andExpect(jsonPath("$[0].email").value("luis@test.com"))
                .andExpect(jsonPath("$[0].nivel").value("INTERMEDIO"))
                .andExpect(jsonPath("$[0].usuario").doesNotExist());
        when(profesores.listarProfesores()).thenReturn(List.of(ProfesorResponse.builder().id(7L)
                .nombre("Ana").apellido("Paz").email("ana@test.com").especialidad("Fuerza").build()));
        mvc.perform(get("/api/profesores")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7)).andExpect(jsonPath("$[0].especialidad").value("Fuerza"));
    }

    @Test
    void exerciseListStillSupportsNameFilterAndReturnsRequiredId() throws Exception {
        when(ejercicios.listarPorNombre("press")).thenReturn(List.of(EjercicioResponse.builder()
                .id(9L).nombre("Press").descripcion("Banca").grupoMuscular(GrupoMuscular.PECHO).build()));
        mvc.perform(get("/api/ejercicios").param("nombre", "press")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9)).andExpect(jsonPath("$[0].nombre").value("Press"))
                .andExpect(jsonPath("$[0].descripcion").value("Banca"))
                .andExpect(jsonPath("$[0].grupoMuscular").value("PECHO"));
    }

    @Test
    void routineHistoryKeepsArrayAndOldFieldsWhileAddingAssignmentTimeline() throws Exception {
        var history = RutinaResponse.builder().id(10L).nombre("Fuerza").descripcion("Plan")
                .dias(List.of()).profesor("Ana").profesorId(7L).alumnos(List.of())
                .asignacionId(20L).fechaInicio(LocalDateTime.of(2026, 9, 1, 10, 0))
                .fechaFin(LocalDateTime.of(2026, 10, 1, 10, 0)).activa(false).build();
        when(rutinas.getHistorialMe(any())).thenReturn(List.of(history));
        when(rutinas.getHistorialDeAlumno(eq(8L), any())).thenReturn(List.of(history));
        for (String path : List.of("/api/rutinas/me/historial", "/api/rutinas/historial/alumno/8")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(10)).andExpect(jsonPath("$[0].nombre").value("Fuerza"))
                    .andExpect(jsonPath("$[0].dias").isArray()).andExpect(jsonPath("$[0].profesor").value("Ana"))
                    .andExpect(jsonPath("$[0].alumnos").isArray())
                    .andExpect(jsonPath("$[0].asignacionId").value(20))
                    .andExpect(jsonPath("$[0].fechaInicio").isNotEmpty())
                    .andExpect(jsonPath("$[0].fechaFin").isNotEmpty())
                    .andExpect(jsonPath("$[0].activa").value(false));
        }
    }

    @Test
    void assignmentEndpointRetainsStatusAndNames() throws Exception {
        when(rutinas.asignarRutinaMe(eq(8L), eq(10L), any())).thenReturn(AsignacionResponse.builder()
                .id(20L).alumnoId(8L).rutinaId(10L).profesorId(7L)
                .alumnoNombre("Luis Rios").rutinaNombre("Fuerza").activa(true).build());
        mvc.perform(post("/api/rutinas/asignar/alumno/8/rutina/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20)).andExpect(jsonPath("$.alumnoId").value(8))
                .andExpect(jsonPath("$.rutinaId").value(10)).andExpect(jsonPath("$.profesorId").value(7))
                .andExpect(jsonPath("$.alumnoNombre").value("Luis Rios"))
                .andExpect(jsonPath("$.rutinaNombre").value("Fuerza")).andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void trainingCreationAndSeriesRegistrationKeepPathsStatusAndResults() throws Exception {
        when(sesiones.iniciarSesion(any(), any())).thenReturn(SesionResponse.builder().id(30L)
                .alumnoId(8L).rutinaId(10L).diaRutinaId(11L).rutinaNombre("Fuerza")
                .diaRutinaNombre("Día 1").fecha(LocalDate.of(2026, 10, 1)).series(List.of()).build());
        mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"diaRutinaId\":11,\"fecha\":\"2026-10-01\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.alumnoId").value(8)).andExpect(jsonPath("$.rutinaId").value(10))
                .andExpect(jsonPath("$.diaRutinaId").value(11)).andExpect(jsonPath("$.series").isArray());
        when(sesiones.registrarSerie(eq(30L), any(), any())).thenReturn(SerieResponse.builder().id(31L)
                .sesionId(30L).diaEjercicioId(12L).ejercicioId(9L).ejercicioNombre("Press")
                .numeroSerie(1).repsHechas(8).pesoUsado(42.5).completada(true).build());
        mvc.perform(post("/api/sesiones/30/series").contentType(MediaType.APPLICATION_JSON)
                .content("{\"diaEjercicioId\":12,\"numeroSerie\":1,\"repsHechas\":8,\"pesoUsado\":42.5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ejercicioId").value(9))
                .andExpect(jsonPath("$.diaEjercicioId").value(12)).andExpect(jsonPath("$.sesionId").value(30))
                .andExpect(jsonPath("$.repsHechas").value(8)).andExpect(jsonPath("$.pesoUsado").value(42.5))
                .andExpect(jsonPath("$.completada").value(true));
    }

    @Test
    void studentMeStillAcceptsExistingBirthDateProperty() throws Exception {
        when(alumnos.updateMiPerfil(any(), any())).thenReturn(AlumnoResponse.builder().id(8L).nombre("Luis").build());
        mvc.perform(put("/api/alumnos/me").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Luis","apellido":"Rios","email":"contacto@test.com",
                 "fechaDeNacimiento":"2001-01-01","nivel":"INTERMEDIO","objetivo":"MEJORAR_RESISTENCIA"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(8));
        verify(alumnos).updateMiPerfil(argThat(r -> LocalDate.of(2001, 1, 1).equals(r.getFechaDeNacimiento())
                && "Rios".equals(r.getApellido())), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "PROFESOR", "ALUMNO"})
    void singleLoginRetainsAllThreeRolesAndResponseContract(String role) throws Exception {
        when(auth.login(any())).thenReturn(AuthResponse.builder().token("test-token")
                .email("login@test.com").rol(role).perfilId(role.equals("ADMIN") ? null : 8L).build());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"login@test.com\",\"password\":\"example\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.rol").value(role)).andExpect(jsonPath("$.email").value("login@test.com"));
    }
}
