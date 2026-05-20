package utn.simulacro_nombreAlumno.model.request;


import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaRequest {

    private String nombre;
    private String descripcion;
    /// ids de ejercicios que componen la rutina???
    private List<Long> ejercicioIds;
    private Long profesorId;
    private Long alumnoId;
}

