package utn.simulacro_nombreAlumno.model.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String especialidad;
}

