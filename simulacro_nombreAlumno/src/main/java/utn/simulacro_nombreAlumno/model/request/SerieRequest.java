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

    @NotNull
    @Positive
    private Integer repsHechas;

    @PesoValido
    private Double pesoUsado;
}