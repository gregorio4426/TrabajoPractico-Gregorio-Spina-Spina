package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaEjercicioRequest {

    @NotNull
    private Long ejercicioId;

    @NotNull
    private Integer seriesSugeridas;

    @NotNull
    private Integer repsSugeridas;

    private Double pesoSugerido;
}