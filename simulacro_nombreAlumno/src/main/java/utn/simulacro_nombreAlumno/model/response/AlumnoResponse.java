package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumnoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Integer edad;
    private LocalDate fechaNacimiento;
}

