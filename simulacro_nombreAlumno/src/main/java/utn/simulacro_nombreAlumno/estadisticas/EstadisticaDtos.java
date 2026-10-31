package utn.simulacro_nombreAlumno.estadisticas;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import utn.simulacro_nombreAlumno.admin.AdminDtos;
import utn.simulacro_nombreAlumno.model.response.AsignacionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class EstadisticaDtos {
    private EstadisticaDtos() {}
    @Schema(description="Ventana inclusiva de fechas efectivas de entrenamiento. Máximo 366 días; por defecto últimos 30 días, usando UTC como fecha de referencia.")
    public record Periodo(LocalDate desde, LocalDate hasta, long dias, String referenciaFecha) {}
    @Schema(description="Resultados observados. Sesión finalizada no implica haber cumplido todo el día prescrito. Solo se suman series válidas de sesiones finalizadas. Los decimales se transmiten como strings para evitar pérdida de precisión en JavaScript.")
    public record Metricas(
            long sesionesRegistradas, long sesionesAbiertas, long sesionesFinalizadas,
            @Schema(description="Sesiones finalizadas con al menos una serie vigente, completada y fiable") long entrenamientosConDatosValidos,
            long sesionesFinalizadasSinDatosValidos, long alumnosConEntrenamiento, long diasConEntrenamiento,
            LocalDate ultimaFechaConEntrenamiento, long seriesValidas, long seriesExcluidas, long seriesEnSesionesAbiertas,
            long repeticionesTotales, Integer repeticionesMaximasPorSerie, long seriesConPeso, long seriesSinPeso,
            @Schema(implementation=String.class,type="string",format="decimal",nullable=true,description="Máximo peso registrado. Null si no se informó ninguno; cero es un peso conocido.")
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal pesoMaximo,
            @Schema(implementation=String.class,type="string",format="decimal",nullable=true,description="Suma exacta de peso registrado × repeticiones. Null si no hay pesos conocidos. No incluye pesos sugeridos ni supone una unidad no registrada.")
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal volumenRegistrado,
            @Schema(description="True solo si hay series válidas y todas informaron peso. False con datos ausentes o volumen parcial.") boolean volumenCompleto) {}
    @Schema(description="Frecuencia observada normalizada a 7 días: conteo × 7 / días inclusivos del período. No es asistencia ni proyección de resultados futuros.")
    public record Frecuencia(@Schema(implementation=String.class,type="string",format="decimal") @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal entrenamientosPorSemana,
            @Schema(implementation=String.class,type="string",format="decimal") @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal diasEntrenadosPorSemana) {}
    @Schema(name="ResumenEstadistico")
    public record Resumen(Periodo periodo, Long alumnoId, Metricas datos, Frecuencia frecuencia) {}
    @Schema(description="Cada fila es un día con sesiones registradas; separa abiertas, finalizadas y válidas. No es asistencia y los días sin registros no generan filas.")
    public record ActividadDia(LocalDate fecha, Metricas datos) {}
    public record EjercicioEstadistica(Long ejercicioId, String nombreActual, Metricas datos) {}
    @Schema(description="Punto por sesión y ejercicio, ordenado por fecha efectiva e ID de sesión ascendentes; conserva nombre histórico y fecha original.")
    public record EvolucionEjercicio(Long ejercicioId, String nombreHistorico, Long sesionId, Long alumnoId,
            LocalDate fechaOriginal, LocalDate fechaEfectiva, Metricas datos) {}
    public record AlumnoEstadistica(Long alumnoId, String nombre, String apellido, Metricas datos, Frecuencia frecuencia) {}
    public record PaginaEstadistica<T>(Periodo periodo, List<T> contenido, int pagina, int tamanio,
            long totalElementos, long totalPaginas) {}
    @Schema(description="Estado actual de cuentas/plantillas y entrenamiento del período. No se filtran cuentas por fecha de alta: el backend no conserva ese dato.")
    public record DashboardAdmin(Periodo periodo, AdminDtos.Resumen estadoActual, Metricas entrenamientos, Frecuencia frecuencia) {}
    public record DashboardProfesor(Periodo periodo, Long profesorId, long alumnosVinculados,
            long rutinasPropias, long rutinasPropiasArchivadas, long asignacionesActivasDeSusAlumnos, Metricas entrenamientos, Frecuencia frecuencia) {}
    public record DashboardAlumno(Periodo periodo, Long alumnoId,
            @Schema(nullable=true,description="Asignación activa y su versión exacta; null si no tiene asignación activa") AsignacionResponse rutinaActiva,
            Metricas entrenamientos, Frecuencia frecuencia) {}
}
