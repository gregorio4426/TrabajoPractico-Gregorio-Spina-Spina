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
public class RegisterAlumnoRequest {


    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;


    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @Positive
    @NotNull
    private Integer edad;

    @PastOrPresent
    private LocalDate fechaNacimiento;

    @NotNull
    private Nivel nivel;

    @NotNull
    private Objetivo objetivo;
}
