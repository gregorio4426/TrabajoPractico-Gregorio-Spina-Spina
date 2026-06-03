package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.request.AlumnoRequest;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {
    Alumno toEntity(AlumnoRequest request);
    AlumnoResponse toDto(Alumno alumno);

    List<AlumnoResponse> toLISTDto(List<Alumno> alumnos);
}

