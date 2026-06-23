package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.cglib.core.Local;
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

    @Past
    private LocalDate FechaDeNacimiento;

    @NotNull
    private Nivel nivel;

    @NotNull
    private Objetivo objetivo;
}

