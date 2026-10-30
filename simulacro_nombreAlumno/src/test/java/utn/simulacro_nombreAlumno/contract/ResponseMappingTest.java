package utn.simulacro_nombreAlumno.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import utn.simulacro_nombreAlumno.mapper.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.response.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMappingTest {
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
    private final AlumnoMapper alumnos = Mappers.getMapper(AlumnoMapper.class);
    private final ProfesorMapper profesores = Mappers.getMapper(ProfesorMapper.class);
    private final RutinaMapper rutinas = Mappers.getMapper(RutinaMapper.class);
    private final SesionMapper sesiones = Mappers.getMapper(SesionMapper.class);
    private final AsignacionMapper asignaciones = Mappers.getMapper(AsignacionMapper.class);
    private final Profesor profesor = Profesor.builder().id(7L).nombre("Ana").apellido("Paz")
            .email("ana@test.com").especialidad("Fuerza").build();
    private final Alumno alumno = Alumno.builder().id(8L).nombre("Luis").apellido("Rios")
            .email("luis@test.com").edad(25).fechaNacimiento(LocalDate.of(2001, 1, 1))
            .nivel(Nivel.INTERMEDIO).objetivo(Objetivo.GANAR_MASA_MUSCULAR)
            .profesores(List.of(profesor)).build();
    private final Ejercicio ejercicio = Ejercicio.builder().id(9L).nombre("Press")
            .descripcion("Press de banca").grupoMuscular(GrupoMuscular.PECHO).build();
    private final Rutina rutina = Rutina.builder().id(10L).nombre("Fuerza")
            .descripcion("Tres días").profesor(profesor).build();
    private final DiaRutina dia = DiaRutina.builder().id(11L).nombre("Día 1")
            .numeroOrden(1).rutina(rutina).build();
    private final DiaEjercicio plan = DiaEjercicio.builder().id(12L).diaRutina(dia)
            .ejercicio(ejercicio).seriesSugeridas(3).repsSugeridas(8).pesoSugerido(40.0).build();

    @Test
    void profilesIncludeIdsAndFiniteRelationshipsWithoutCredentials() {
        Usuario usuario = Usuario.builder().id(1L).email("login@test.com").password("NEVER_EXPOSE").rol(Rol.ALUMNO).build();
        alumno.setUsuario(usuario);
        usuario.setAlumno(alumno);
        profesor.setAlumnos(List.of(alumno));
        profesor.setRutinas(List.of(rutina));
        JsonNode a = json.valueToTree(alumnos.toDto(alumno));
        assertThat(a.get("id").asLong()).isEqualTo(8L);
        assertThat(a.get("nombre").asText()).isEqualTo("Luis");
        assertThat(a.get("apellido").asText()).isEqualTo("Rios");
        assertThat(a.get("email").asText()).isEqualTo("luis@test.com");
        assertThat(a.get("edad").asInt()).isEqualTo(25);
        assertThat(a.get("fechaNacimiento")).isNotNull();
        assertThat(a.get("nivel").asText()).isEqualTo("INTERMEDIO");
        assertThat(a.get("objetivo").asText()).isEqualTo("GANAR_MASA_MUSCULAR");
        assertThat(a.at("/profesores/0/id").asLong()).isEqualTo(7L);
        assertThat(a.at("/profesores/0/alumnos").isMissingNode()).isTrue();
        assertThat(a.toString()).doesNotContain("password", "usuario", "NEVER_EXPOSE");
        JsonNode p = json.valueToTree(profesores.toDto(profesor));
        assertThat(p.get("id").asLong()).isEqualTo(7L);
        assertThat(p.get("especialidad").asText()).isEqualTo("Fuerza");
        assertThat(p.toString()).doesNotContain("password", "usuario", "alumnos", "rutinas");
    }

    @Test
    void exerciseAndRoutineKeepOldFieldsAndExposeStableIds() {
        var e = Mappers.getMapper(EjercicioMapper.class).toDto(ejercicio);
        assertThat(e.getId()).isEqualTo(9L);
        assertThat(e.getGrupoMuscular()).isEqualTo(GrupoMuscular.PECHO);
        assertThat(e.getNombre()).isEqualTo("Press");
        assertThat(e.getDescripcion()).isEqualTo("Press de banca");
        dia.setEjercicios(List.of(plan));
        rutina.setDias(List.of(dia));
        var r = rutinas.toDto(rutina);
        assertThat(r.getId()).isEqualTo(10L);
        assertThat(r.getProfesorId()).isEqualTo(7L);
        assertThat(r.getProfesor()).isEqualTo("Ana");
        assertThat(r.getDescripcion()).isEqualTo("Tres días");
        assertThat(r.getDias().getFirst().getId()).isEqualTo(11L);
        assertThat(r.getDias().getFirst().getEjercicios().getFirst().getId()).isEqualTo(12L);
        assertThat(r.getDias().getFirst().getEjercicios().getFirst().getEjercicioId()).isEqualTo(9L);
        assertThat(r.getDias().getFirst().getEjercicios().getFirst().getGrupoMuscular()).isEqualTo("PECHO");
        assertThat(r.getAsignacionId()).isNull();
        assertThat(r.getActiva()).isNull();
    }

    @Test
    void assignmentNamesIdsAndDatesAreMapped() {
        var a = assignment(20L, false);
        var response = asignaciones.toResponse(a);
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getRutinaId()).isEqualTo(10L);
        assertThat(response.getAlumnoId()).isEqualTo(8L);
        assertThat(response.getProfesorId()).isEqualTo(7L);
        assertThat(response.getAlumnoNombre()).isEqualTo("Luis Rios");
        assertThat(response.getRutinaNombre()).isEqualTo("Fuerza");
        assertThat(response.getFechaInicio()).isEqualTo(a.getFechaInicio());
        assertThat(response.getFechaFin()).isEqualTo(a.getFechaFin());
        assertThat(response.isActiva()).isFalse();
    }

    @Test
    void repeatedAssignmentsOfSameRoutineRetainIndependentHistoryMetadata() {
        var previous = rutinas.toHistorialDto(assignment(20L, false));
        var current = rutinas.toHistorialDto(assignment(21L, true));
        assertThat(previous.getId()).isEqualTo(current.getId());
        assertThat(previous.getAsignacionId()).isEqualTo(20L);
        assertThat(current.getAsignacionId()).isEqualTo(21L);
        assertThat(previous.getActiva()).isFalse();
        assertThat(previous.getFechaFin()).isNotNull();
        assertThat(current.getActiva()).isTrue();
        assertThat(current.getFechaFin()).isNull();
    }

    @Test
    void sessionAndSeriesCarryIdentityAndActualResults() {
        Sesion sesion = Sesion.builder().id(30L).alumno(alumno).diaRutina(dia)
                .fecha(LocalDate.of(2026, 10, 1)).completada(true).build();
        Serie serie = Serie.builder().id(31L).sesion(sesion).diaEjercicio(plan)
                .numeroSerie(1).repsHechas(8).pesoUsado(42.5).completada(true).build();
        sesion.setSeries(List.of(serie));
        var result = sesiones.toDto(sesion);
        assertThat(result.getAlumnoId()).isEqualTo(8L);
        assertThat(result.getRutinaId()).isEqualTo(10L);
        assertThat(result.getDiaRutinaId()).isEqualTo(11L);
        assertThat(result.getRutinaNombre()).isEqualTo("Fuerza");
        assertThat(result.getDiaRutinaNombre()).isEqualTo("Día 1");
        assertThat(result.getFecha()).isEqualTo(sesion.getFecha());
        assertThat(result.isCompletada()).isTrue();
        var s = result.getSeries().getFirst();
        assertThat(s.getId()).isEqualTo(31L);
        assertThat(s.getSesionId()).isEqualTo(30L);
        assertThat(s.getDiaEjercicioId()).isEqualTo(12L);
        assertThat(s.getEjercicioId()).isEqualTo(9L);
        assertThat(s.getEjercicioNombre()).isEqualTo("Press");
        assertThat(s.getRepsHechas()).isEqualTo(8);
        assertThat(s.getPesoUsado()).isEqualTo(42.5);
        assertThat(s.isCompletada()).isTrue();
    }

    @Test
    void missingOptionalAssociationsDoNotProduceInventedIds() {
        assertThat(sesiones.toDto(Sesion.builder().build()).getAlumnoId()).isNull();
        assertThat(sesiones.toSerieDto(Serie.builder().build()).getEjercicioId()).isNull();
        assertThat(asignaciones.toResponse(AsignacionRutina.builder().build()).getRutinaId()).isNull();
        assertThat(asignaciones.toResponse(AsignacionRutina.builder().build()).getAlumnoNombre()).isNull();
        assertThat(alumnos.toDto(Alumno.builder().build()).getProfesores()).isEmpty();
    }

    @Test
    void oldJavaConstructorsRemainAvailable() {
        assertThat(new EjercicioResponse("Press", "Descripción").getNombre()).isEqualTo("Press");
        assertThat(new AlumnoResponse("Luis", "Rios", "a@test.com", 25, null).getId()).isNull();
        assertThat(new ProfesorResponse("Ana", "Paz", "p@test.com", "Fuerza").getId()).isNull();
        assertThat(new AsignacionResponse(null, null, true, "Fuerza", "Luis Rios").isActiva()).isTrue();
        assertThat(new RutinaResponse("Fuerza", "D", List.of(), "Ana", List.of()).getDias()).isEmpty();
        assertThat(new DiaEjercicioResponse(12L, "Press", "PECHO", 3, 8, 40.0).getEjercicioId()).isNull();
        assertThat(new SerieResponse(31L, "Press", 1, 8, 40.0, true).getId()).isEqualTo(31L);
        assertThat(new SesionResponse(30L, null, true, "Día 1", "Fuerza", List.of()).getId()).isEqualTo(30L);
    }

    private AsignacionRutina assignment(Long id, boolean active) {
        return AsignacionRutina.builder().id(id).alumno(alumno).rutina(rutina)
                .fechaInicio(LocalDateTime.of(2026, 9, 1, 12, 0))
                .fechaFin(active ? null : LocalDateTime.of(2026, 10, 1, 12, 0)).activa(active).build();
    }
}
