package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieResponse {

    private Long id;
    private String ejercicioNombre;
    private Integer numeroSerie;
    private Integer repsHechas;
    private Double pesoUsado;
    private boolean completada;
}