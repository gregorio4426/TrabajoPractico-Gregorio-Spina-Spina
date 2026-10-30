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

    private Long id;

    // Conserva el constructor anterior para consumidores Java existentes.
    public ProfesorResponse(String nombre, String apellido, String email, String especialidad) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.especialidad = especialidad;
    }
    private String emailContacto;
}

