package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieRequest {

    @NotNull
    @Positive
    private Long diaEjercicioId;

    @NotNull
    @Positive
    private Integer numeroSerie;

    @Positive
    private Integer repsHechas;

    @PesoValido
    private Double pesoUsado;

    @Positive private Integer segundosHechos;
    @AssertTrue(message="Ingresá repeticiones o segundos positivos, sin mezclar las unidades")
    public boolean isResultadoInformado() { return (repsHechas != null && repsHechas > 0 && segundosHechos == null) || (segundosHechos != null && segundosHechos > 0 && repsHechas == null); }
}