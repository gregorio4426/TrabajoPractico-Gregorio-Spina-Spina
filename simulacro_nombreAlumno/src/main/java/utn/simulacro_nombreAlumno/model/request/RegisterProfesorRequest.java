package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterProfesorRequest {


    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;


    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String especialidad;
}
