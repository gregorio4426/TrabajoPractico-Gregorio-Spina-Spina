package utn.simulacro_nombreAlumno.model.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioRequest {
    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;



}

