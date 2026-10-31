package utn.simulacro_nombreAlumno.estadisticas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.response.ErrorResponse;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaService.Alcance.*;

@RestController @RequestMapping("/api/profesores/me") @RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESOR')") @Tag(name="Estadísticas PROFESOR",description="Consultas de solo lectura con fechas efectivas inclusivas. No representan asistencia ni duración. Volumen = peso registrado × repeticiones, excluyendo series no fiables y sesiones abiertas.")
@ApiResponses({
    @ApiResponse(responseCode="200",description="Consulta correcta; datos reales del ámbito autorizado",useReturnTypeSchema=true),
    @ApiResponse(responseCode="400",description="Filtros inválidos, rango mayor de 366 días o página inválida",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="401",description="JWT ausente, vencido, revocado o cuenta inactiva",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="403",description="Rol no permitido o alumno ajeno/no vinculado",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="404",description="Alumno o ejercicio autorizado inexistente",content=@Content(schema=@Schema(implementation=ErrorResponse.class)))
})
public class ProfesorEstadisticaController {
    private final EstadisticaService service;
    @GetMapping("/dashboard") @Operation(summary="Dashboard PROFESOR",description="Estado actual del perfil/gimnasio y métricas del período. Por defecto últimos 30 días. No implica cumplimiento completo de una rutina.")
    public DashboardProfesor dashboard(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.dashboardProfesor(f,auth);}
    @GetMapping("/estadisticas") @Operation(summary="Resumen de entrenamientos, exclusiones y cargas del período")
    public Resumen resumen(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.resumen(MIS_ALUMNOS,null,f,auth);}
    @GetMapping("/estadisticas/actividad") @Operation(summary="Actividad diaria paginada, en orden de fecha ascendente")
    public PaginaEstadistica<ActividadDia> actividad(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.actividad(MIS_ALUMNOS,null,f,auth);}
    @GetMapping("/estadisticas/ejercicios") @Operation(summary="Ejercicios con datos válidos, paginados por ID ascendente")
    public PaginaEstadistica<EjercicioEstadistica> ejercicios(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.ejercicios(MIS_ALUMNOS,null,f,auth);}
    @GetMapping("/estadisticas/ejercicios/{ejercicioId}/evolucion") @Operation(summary="Evolución observada por sesión y ejercicio, ordenada por fecha efectiva e ID ascendentes")
    public PaginaEstadistica<EvolucionEjercicio> evolucion(@PathVariable @Positive Long ejercicioId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.evolucion(MIS_ALUMNOS,null,ejercicioId,f,auth);}
    @GetMapping("/estadisticas/alumnos") @Operation(summary="Estadísticas de mis alumnos vinculados, incluyendo alumnos sin entrenamientos")
    public PaginaEstadistica<AlumnoEstadistica> alumnos(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.alumnosProfesor(f,auth);}
    @GetMapping("/alumnos/{alumnoId}/estadisticas") @Operation(summary="Estadísticas de un alumno autorizado; incluye todo su historial sin filtrar por autor de rutina")
    public Resumen resumenAlumno(@PathVariable @Positive Long alumnoId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.resumen(ALUMNO,alumnoId,f,auth);}
    @GetMapping("/alumnos/{alumnoId}/estadisticas/actividad") @Operation(summary="Actividad del alumno por fecha efectiva")
    public PaginaEstadistica<ActividadDia> actividadAlumno(@PathVariable @Positive Long alumnoId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.actividad(ALUMNO,alumnoId,f,auth);}
    @GetMapping("/alumnos/{alumnoId}/estadisticas/ejercicios") @Operation(summary="Cargas y volumen por ejercicio del alumno")
    public PaginaEstadistica<EjercicioEstadistica> ejerciciosAlumno(@PathVariable @Positive Long alumnoId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.ejercicios(ALUMNO,alumnoId,f,auth);}
    @GetMapping("/alumnos/{alumnoId}/estadisticas/ejercicios/{ejercicioId}/evolucion") @Operation(summary="Evolución histórica del ejercicio para el alumno autorizado")
    public PaginaEstadistica<EvolucionEjercicio> evolucionAlumno(@PathVariable @Positive Long alumnoId,@PathVariable @Positive Long ejercicioId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.evolucion(ALUMNO,alumnoId,ejercicioId,f,auth);}
}
