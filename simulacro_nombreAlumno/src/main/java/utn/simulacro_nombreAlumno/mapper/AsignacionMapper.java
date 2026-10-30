package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import utn.simulacro_nombreAlumno.model.AsignacionRutina;
import utn.simulacro_nombreAlumno.model.response.AsignacionResponse;

@Mapper(componentModel = "spring")
public interface AsignacionMapper {

    @Mapping(target = "rutinaId", source = "rutina.idRaiz")
    @Mapping(target = "rutinaVersionId", source = "rutina.id")
    @Mapping(target = "numeroVersion", source = "rutina.numeroVersion")
    @Mapping(target = "archivada", source = "rutina.archivadaEfectiva")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "profesorId", source = "rutina.profesor.id")
    @Mapping(target = "rutinaNombre", source = "rutina.nombre")
    @Mapping(target = "alumnoNombre", expression = "java(asignacionRutina.getAlumno() == null ? null : asignacionRutina.getAlumno().getNombre() + \" \" + asignacionRutina.getAlumno().getApellido())")
    AsignacionResponse toResponse (AsignacionRutina asignacionRutina);

}
