package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorRequest {
   @NotBlank
    private String nombre;

   @NotBlank
    private String apellido;

   @Email
    private String email;

    @NotBlank
    private String especialidad;
}
