package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaEjercicioResponse {

    private Long id;
    private String ejercicioNombre;
    private String grupoMuscular;
    private Integer seriesSugeridas;
    private Integer repsSugeridas;
    private Double pesoSugerido;
}