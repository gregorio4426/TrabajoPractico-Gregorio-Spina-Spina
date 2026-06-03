package utn.simulacro_nombreAlumno.mapper;

import org.mapstruct.Mapper;
import utn.simulacro_nombreAlumno.model.Ejercicio;
import utn.simulacro_nombreAlumno.model.request.EjercicioRequest;
import utn.simulacro_nombreAlumno.model.response.EjercicioResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EjercicioMapper {
    Ejercicio toEntity(EjercicioRequest request);
    EjercicioResponse toDto(Ejercicio ejercicio);

    List<EjercicioResponse> toLISTDto(List<Ejercicio> ejercicios);
}

