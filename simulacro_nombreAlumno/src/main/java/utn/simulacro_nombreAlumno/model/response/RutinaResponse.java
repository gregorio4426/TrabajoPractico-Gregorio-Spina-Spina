package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Set<EjercicioResponse> ejercicios;
    private Long profesorId;
    private Long alumnoId;
}

