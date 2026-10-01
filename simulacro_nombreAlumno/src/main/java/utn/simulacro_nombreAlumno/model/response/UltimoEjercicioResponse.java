package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UltimoEjercicioResponse {
    private Long sesionId;
    private LocalDate fecha;
    private Long ejercicioId;
    private String ejercicioNombre;
    private List<SerieResponse> series;
}
