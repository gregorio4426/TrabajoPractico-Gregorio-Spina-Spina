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
    RutinaResponse toDto(Rutina rutina);
    
    Rutina toEntity(RutinaRequest request);

    List<RutinaResponse> toLISTDto(List<Rutina> rutinas);
}

