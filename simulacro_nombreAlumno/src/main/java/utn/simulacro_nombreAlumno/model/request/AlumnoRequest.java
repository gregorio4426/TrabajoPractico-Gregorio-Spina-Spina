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

    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @Email
    private String email;

    @Positive
    @NotNull
    private Integer edad;

    @FutureOrPresent
    private LocalDate fechaNacimiento;
}

