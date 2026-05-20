package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumnoRequest {
    private String nombre;
    private String apellido;
    private String email;
    private Integer edad;
    private LocalDate fechaNacimiento;
}

