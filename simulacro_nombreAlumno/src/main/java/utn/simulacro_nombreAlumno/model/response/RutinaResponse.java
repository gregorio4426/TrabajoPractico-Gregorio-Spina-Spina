package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaResponse {

    private String nombre;
    private String descripcion;
    private List<EjercicioResponse> ejercicios;
    private String profesor;
    private List<String> alumnos;
}

