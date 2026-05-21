package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;       /// pq esta aca si no esta en le entity de ejercicio
    private Integer caloriasEstimadas;     /// ...
}

