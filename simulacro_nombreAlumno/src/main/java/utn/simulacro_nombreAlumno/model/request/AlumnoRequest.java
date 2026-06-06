package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;
import utn.simulacro_nombreAlumno.model.Nivel;
import utn.simulacro_nombreAlumno.model.Objetivo;

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

    @PastOrPresent
    private LocalDate fechaNacimiento;

    ///  nose si van validaciones aca
    @NotNull
    private Nivel nivel;
 /// nose si va validaciones aca
    @NotNull
    private Objetivo objetivo;
}

