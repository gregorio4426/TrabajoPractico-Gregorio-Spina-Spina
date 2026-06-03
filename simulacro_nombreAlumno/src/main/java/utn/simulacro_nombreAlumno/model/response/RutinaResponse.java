package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaResponse {

    private String nombre;
    private String descripcion;
    private Set<EjercicioResponse> ejercicios;
    private String profesor;
    private String alumno;
}

