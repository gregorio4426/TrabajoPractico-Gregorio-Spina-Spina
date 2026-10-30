package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import utn.simulacro_nombreAlumno.model.Sesion;
import utn.simulacro_nombreAlumno.model.Serie;
import utn.simulacro_nombreAlumno.model.response.SerieResponse;
import utn.simulacro_nombreAlumno.model.response.SesionResponse;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SesionMapper {

    @Mapping(target = "finalizada", source = "completada")
    @Mapping(target = "diaCumplido", ignore = true)
    @Mapping(target = "seriesEsperadas", ignore = true)
    @Mapping(target = "seriesRealizadas", ignore = true)
    @Mapping(target = "diaRutinaNombre", source = "diaRutina.nombre")
    @Mapping(target = "rutinaNombre", source = "diaRutina.rutina.nombre")
    @Mapping(target = "series", source = "series")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "rutinaId", source = "diaRutina.rutina.idRaiz")
    @Mapping(target = "rutinaVersionId", source = "diaRutina.rutina.id")
    @Mapping(target = "numeroVersion", source = "diaRutina.rutina.numeroVersion")
    @Mapping(target = "asignacionId", source = "asignacion.id")
    @Mapping(target = "diaRutinaId", source = "diaRutina.id")
    SesionResponse toDto(Sesion sesion);

    @Mapping(target = "ejercicioNombre", source = "diaEjercicio.nombreHistorico")
    @Mapping(target = "fechaEfectiva", source = "sesion.fechaEfectiva")
    @Mapping(target = "fecha", source = "sesion.fecha")
    @Mapping(target = "sesionId", source = "sesion.id")
    @Mapping(target = "diaEjercicioId", source = "diaEjercicio.id")
    @Mapping(target = "ejercicioId", source = "diaEjercicio.ejercicio.id")
    SerieResponse toSerieDto(Serie serie);

    List<SesionResponse> toLISTDto(List<Sesion> sesiones);

    @org.mapstruct.AfterMapping
    default void cumplimiento(Sesion source, @org.mapstruct.MappingTarget SesionResponse target) {
        var resultado = utn.simulacro_nombreAlumno.model.CumplimientoEntrenamiento.evaluar(source);
        target.setDiaCumplido(resultado.diaCumplido());
        target.setSeriesEsperadas(resultado.seriesEsperadas());
        target.setSeriesRealizadas(resultado.seriesRealizadas());
    }
}
