package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieRequest {

    @NotNull
    private Long diaEjercicioId;

    @NotNull
    private Integer numeroSerie;

    private Integer repsHechas;

    private Double pesoUsado;
}