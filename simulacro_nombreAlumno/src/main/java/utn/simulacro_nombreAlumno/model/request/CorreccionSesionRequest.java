package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorreccionSesionRequest {
    @NotNull @PastOrPresent private java.time.LocalDate fecha;
    @NotBlank @Size(max = 500) private String motivo;
    @NotNull @PositiveOrZero private Long revision;
}
