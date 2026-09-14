package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionRequest {

    @NotNull
    private Long diaRutinaId;

    @NotNull
    private LocalDate fecha;
}