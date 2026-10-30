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

@RestController @RequestMapping("/api/alumnos/me") @RequiredArgsConstructor
@PreAuthorize("hasRole('ALUMNO')") @Tag(name="Estadísticas ALUMNO",description="Consultas de solo lectura con fechas efectivas inclusivas. No representan asistencia ni duración. Volumen = peso registrado × repeticiones, excluyendo series no fiables y sesiones abiertas.")
@ApiResponses({
    @ApiResponse(responseCode="200",description="Consulta correcta; datos reales del ámbito autorizado",useReturnTypeSchema=true),
    @ApiResponse(responseCode="400",description="Filtros inválidos, rango mayor de 366 días o página inválida",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="401",description="JWT ausente, vencido, revocado o cuenta inactiva",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="403",description="Rol no permitido o alumno ajeno/no vinculado",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
    @ApiResponse(responseCode="404",description="Alumno o ejercicio autorizado inexistente",content=@Content(schema=@Schema(implementation=ErrorResponse.class)))
})
public class AlumnoEstadisticaController {
    private final EstadisticaService service;
    @GetMapping("/dashboard") @Operation(summary="Dashboard ALUMNO",description="Estado actual del perfil/gimnasio y métricas del período. Por defecto últimos 30 días. No implica cumplimiento completo de una rutina.")
    public DashboardAlumno dashboard(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.dashboardAlumno(f,auth);}
    @GetMapping("/estadisticas") @Operation(summary="Resumen de entrenamientos, exclusiones y cargas del período")
    public Resumen resumen(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.resumen(PROPIO,null,f,auth);}
    @GetMapping("/estadisticas/actividad") @Operation(summary="Actividad diaria paginada, en orden de fecha ascendente")
    public PaginaEstadistica<ActividadDia> actividad(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.actividad(PROPIO,null,f,auth);}
    @GetMapping("/estadisticas/ejercicios") @Operation(summary="Ejercicios con datos válidos, paginados por ID ascendente")
    public PaginaEstadistica<EjercicioEstadistica> ejercicios(@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.ejercicios(PROPIO,null,f,auth);}
    @GetMapping("/estadisticas/ejercicios/{ejercicioId}/evolucion") @Operation(summary="Evolución observada por sesión y ejercicio, ordenada por fecha efectiva e ID ascendentes")
    public PaginaEstadistica<EvolucionEjercicio> evolucion(@PathVariable @Positive Long ejercicioId,@Valid @ParameterObject @ModelAttribute EstadisticaFiltro f,Authentication auth) {return service.evolucion(PROPIO,null,ejercicioId,f,auth);}
}
