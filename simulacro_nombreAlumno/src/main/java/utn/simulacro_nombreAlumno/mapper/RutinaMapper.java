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
    @Mapping(target = "id", source = "idRaiz")
    @Mapping(target = "rutinaVersionId", source = "id")
    @Mapping(target = "archivada", source = "archivadaEfectiva")
    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "asignacionId", ignore = true)
    @Mapping(target = "fechaInicio", ignore = true)
    @Mapping(target = "fechaFin", ignore = true)
    @Mapping(target = "activa", ignore = true)
    RutinaResponse toDto(Rutina rutina);

    @Mapping(target = "ejercicios", source = "ejercicios")
    DiaRutinaResponse toDiaDto(DiaRutina diaRutina);

    @Mapping(target = "ejercicioNombre", source = "nombreHistorico")
    @Mapping(target = "grupoMuscular", source = "grupoHistorico")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    DiaEjercicioResponse toDiaEjercicioDto(DiaEjercicio diaEjercicio);

    /** Mantiene el array de rutinas y añade el contexto de cada asignación. */
    default RutinaResponse toHistorialDto(utn.simulacro_nombreAlumno.model.AsignacionRutina asignacion) {
        RutinaResponse response = toDto(asignacion.getRutina());
        response.setAsignacionId(asignacion.getId());
        response.setFechaInicio(asignacion.getFechaInicio());
        response.setFechaFin(asignacion.getFechaFin());
        response.setActiva(asignacion.isActiva());
        return response;
    }

    Rutina toEntity(RutinaRequest request);

    List<RutinaResponse> toLISTDto(List<Rutina> rutinas);
}

