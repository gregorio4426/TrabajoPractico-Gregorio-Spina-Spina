package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {
    Profesor toEntity(ProfesorRequest request);
    ProfesorResponse toDto(Profesor profesor);

    List<ProfesorResponse> toLISTDto(List<Profesor> profesores);
}

