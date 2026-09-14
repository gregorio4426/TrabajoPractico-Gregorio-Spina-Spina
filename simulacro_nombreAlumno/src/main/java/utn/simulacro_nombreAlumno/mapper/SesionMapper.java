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

    @Mapping(target = "diaRutinaNombre", source = "diaRutina.nombre")
    @Mapping(target = "rutinaNombre", source = "diaRutina.rutina.nombre")
    @Mapping(target = "series", source = "series")
    SesionResponse toDto(Sesion sesion);

    @Mapping(target = "ejercicioNombre", source = "diaEjercicio.ejercicio.nombre")
    SerieResponse toSerieDto(Serie serie);

    List<SesionResponse> toLISTDto(List<Sesion> sesiones);
}