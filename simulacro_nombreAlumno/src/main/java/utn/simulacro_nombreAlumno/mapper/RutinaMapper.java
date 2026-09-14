package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import utn.simulacro_nombreAlumno.model.DiaEjercicio;
import utn.simulacro_nombreAlumno.model.DiaRutina;
import utn.simulacro_nombreAlumno.model.Rutina;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.DiaEjercicioResponse;
import utn.simulacro_nombreAlumno.model.response.DiaRutinaResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RutinaMapper {

    @Mapping(target = "profesor", expression = "java(rutina.getProfesor() != null ? rutina.getProfesor().getNombre() : null)")
    @Mapping(target = "alumnos", expression = "java(rutina.getAsignaciones().stream().filter(a -> a.isActiva()).map(a -> a.getAlumno().getNombre() + \" \" + a.getAlumno().getApellido()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "dias", source = "dias")
    RutinaResponse toDto(Rutina rutina);

    @Mapping(target = "ejercicios", source = "ejercicios")
    DiaRutinaResponse toDiaDto(DiaRutina diaRutina);

    @Mapping(target = "ejercicioNombre", source = "ejercicio.nombre")
    @Mapping(target = "grupoMuscular", expression = "java(diaEjercicio.getEjercicio().getGrupoMuscular() != null ? diaEjercicio.getEjercicio().getGrupoMuscular().name() : null)")
    DiaEjercicioResponse toDiaEjercicioDto(DiaEjercicio diaEjercicio);

    Rutina toEntity(RutinaRequest request);

    List<RutinaResponse> toLISTDto(List<Rutina> rutinas);
}

