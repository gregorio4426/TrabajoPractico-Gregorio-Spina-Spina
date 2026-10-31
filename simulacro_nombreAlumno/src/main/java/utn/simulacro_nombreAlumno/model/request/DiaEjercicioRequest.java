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
public class DiaEjercicioRequest {

    @NotNull
    @Positive
    private Long ejercicioId;

    @NotNull
    @Positive
    private Integer seriesSugeridas;

    @NotNull
    @Positive
    private Integer repsSugeridas;

    @PesoValido
    private Double pesoSugerido;
}