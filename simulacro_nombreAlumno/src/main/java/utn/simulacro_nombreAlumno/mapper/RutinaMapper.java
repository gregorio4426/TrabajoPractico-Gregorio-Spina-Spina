package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import utn.simulacro_nombreAlumno.model.Rutina;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;

@Mapper(componentModel = "spring")
public interface RutinaMapper {
    RutinaResponse toDto (Rutina rutina);
    Rutina toEntity(RutinaRequest request);
}

