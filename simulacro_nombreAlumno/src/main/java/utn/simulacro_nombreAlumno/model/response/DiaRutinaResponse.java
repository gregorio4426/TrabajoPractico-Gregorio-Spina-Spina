package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaRutinaResponse {

    private Long id;
    private String nombre;
    private Integer numeroOrden;
    private List<DiaEjercicioResponse> ejercicios;
}
