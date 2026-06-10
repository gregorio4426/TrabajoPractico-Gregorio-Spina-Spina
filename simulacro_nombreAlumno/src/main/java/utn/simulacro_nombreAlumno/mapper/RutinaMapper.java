package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import utn.simulacro_nombreAlumno.model.Rutina;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RutinaMapper {
    
    @Mapping(target = "profesor", expression = "java(rutina.getProfesor() != null ? rutina.getProfesor().getNombre() : null)")
    @Mapping(target = "alumnos", expression = "java(rutina.getAsignaciones().stream().map(a -> a.getAlumno().getNombre() + \" \" + a.getAlumno().getApellido()).collect(java.util.stream.Collectors.toList()))")
    RutinaResponse toDto(Rutina rutina);
    
    Rutina toEntity(RutinaRequest request);

    List<RutinaResponse> toLISTDto(List<Rutina> rutinas);
}

