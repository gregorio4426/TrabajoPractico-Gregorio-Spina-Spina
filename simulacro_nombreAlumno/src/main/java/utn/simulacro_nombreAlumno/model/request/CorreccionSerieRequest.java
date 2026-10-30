package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorreccionSerieRequest {
    @NotNull @Positive private Integer numeroSerie;
    @NotNull @Positive private Integer repsHechas;
    @PesoValido private Double pesoUsado;
    @NotBlank @Size(max = 500) private String motivo;
    @NotNull @PositiveOrZero private Long revision;
}
