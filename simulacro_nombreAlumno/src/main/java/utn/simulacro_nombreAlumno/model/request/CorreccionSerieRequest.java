package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.*;
import lombok.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorreccionSerieRequest {
    @NotNull @Positive private Integer numeroSerie;
    @Positive private Integer repsHechas;
    @PesoValido private Double pesoUsado;
    @NotBlank @Size(max = 500) private String motivo;
    @NotNull @PositiveOrZero private Long revision;

    @Positive private Integer segundosHechos;
    @AssertTrue(message="Ingresá repeticiones o segundos positivos, sin mezclar las unidades")
    public boolean isResultadoInformado() { return (repsHechas != null && repsHechas > 0 && segundosHechos == null) || (segundosHechos != null && segundosHechos > 0 && repsHechas == null); }
}
