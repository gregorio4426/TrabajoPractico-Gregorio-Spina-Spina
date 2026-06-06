package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import utn.simulacro_nombreAlumno.model.AsignacionRutina;
import utn.simulacro_nombreAlumno.model.response.AsignacionResponse;

@Mapper(componentModel = "spring")
public interface AsignacionMapper {

    AsignacionResponse toResponse (AsignacionRutina asignacionRutina);

}
