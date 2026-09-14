package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionResponse {

    private Long id;
    private LocalDate fecha;
    private boolean completada;
    private String diaRutinaNombre;
    private String rutinaNombre;
    private List<SerieResponse> series;
}
