package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioResponse {

    private String nombre;
    private String descripcion;

}

