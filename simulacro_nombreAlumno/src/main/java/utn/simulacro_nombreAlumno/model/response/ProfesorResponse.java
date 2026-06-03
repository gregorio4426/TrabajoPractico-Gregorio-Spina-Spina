package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorResponse {

    private String nombre;
    private String apellido;
    private String email;
    private String especialidad;
}

