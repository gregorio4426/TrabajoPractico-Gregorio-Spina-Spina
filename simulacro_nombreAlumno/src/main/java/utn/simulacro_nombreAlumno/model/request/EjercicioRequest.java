package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioRequest {

    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private Integer caloriasEstimadas;
}

